package com.codefork.refine.viaf;

import com.codefork.refine.Cache;
import com.codefork.refine.Config;
import com.codefork.refine.SearchQuery;
import com.codefork.refine.datasource.WebServiceDataSource;
import com.codefork.refine.resources.Result;
import com.codefork.refine.resources.ServiceMetaDataResponse;
import com.codefork.refine.viaf.sources.NonVIAFSource;
import com.codefork.refine.viaf.sources.Source;
import com.codefork.refine.viaf.sources.VIAFSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.util.UriUtils;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * VIAF data source.
 *
 * NOTE: VIAF seems to have a limit of 6 simultaneous
 * requests. To be conservative, we default to 4.
 */
public class VIAF extends WebServiceDataSource {

    public static final String EXTRA_PARAM_SOURCE_FROM_PATH = "sourceFromPath";
    public static final String EXTRA_PARAM_PROXY_MODE = "proxyMode";

    private SAXParserFactory spf;

    Log log = LogFactory.getLog(VIAF.class);

    private VIAFSource viafSource = null;
    private Map<String, NonVIAFSource> nonViafSources = new HashMap<String, NonVIAFSource>();

    @Override
    public void init(Config config) {
        super.init(config);

        spf = SAXParserFactory.newInstance();

        boolean cacheEnabled = Boolean.valueOf(config.getProperties().getProperty("cache.enabled",
                "true"));
        int cacheLifetime = Integer.valueOf(config.getProperties().getProperty("cache.lifetime",
                String.valueOf(Cache.DEFAULT_LIFETIME)));
        int cacheMaxSize = Integer.valueOf(config.getProperties().getProperty("cache.max_size",
                String.valueOf(Cache.DEFAULT_MAXSIZE)));

        setCacheLifetime(cacheLifetime);
        setCacheMaxSize(cacheMaxSize);
        setCacheEnabled(cacheEnabled);
    }

    /**
     * Factory method for getting a NonVIAFSource object
     */
    public NonVIAFSource findNonViafSource(String code) {
        if(!nonViafSources.containsKey(code)) {
            nonViafSources.put(code, new NonVIAFSource(code));
        }
        return nonViafSources.get(code);
    }

    /**
     * Factory method for getting a Source object
     * @param query
     * @return
     */
    public Source findSource(SearchQuery query) {
        String source = query.getExtraParams().get(EXTRA_PARAM_SOURCE_FROM_PATH);
        boolean isProxyMode = Boolean.valueOf(query.getExtraParams().get(EXTRA_PARAM_PROXY_MODE));
        if(!isProxyMode) {
            if(viafSource == null) {
                viafSource = new VIAFSource();
            }
            return viafSource;
        }
        return findNonViafSource(source);
    }

    @Override
    public ServiceMetaDataResponse createServiceMetaDataResponse(Map<String, String> extraParams) {
        // we don't call getName() for service name b/c we reuse this VIAF object
        if(Boolean.valueOf(extraParams.get(EXTRA_PARAM_PROXY_MODE))) {
            return new VIAFProxyModeMetaDataResponse(findNonViafSource(extraParams.get(EXTRA_PARAM_SOURCE_FROM_PATH)));
        }
        return new VIAFMetaDataResponse("VIAF", extraParams.get(EXTRA_PARAM_SOURCE_FROM_PATH));
    }

    public Map<String, String> parseRequestToExtraParams(HttpServletRequest request) {
        String[] parts = request.getServletPath().split("/");
        String dataSourceStr = parts[2];
        String sourceFromPath = null;
        if (parts.length > 3) {
            sourceFromPath = parts[3];
        }

        Map<String, String> extraParams = new HashMap<String, String>();
        extraParams.put(EXTRA_PARAM_SOURCE_FROM_PATH, sourceFromPath);
        // a hack to set proxy mode based on name of data source in URL.
        extraParams.put(EXTRA_PARAM_PROXY_MODE, Boolean.toString("viafproxy".equals(dataSourceStr)));
        return extraParams;
    }

    /**
     * @return String used for the cql 'query' URL param passed to VIAF
     */
    public static String createCqlQueryString(SearchQuery searchQuery) {
        String cqlTemplate = "local.mainHeadingEl all \"%s\"";
        if(searchQuery.getNameType() != null) {
            cqlTemplate = VIAFNameType.getById(searchQuery.getNameType().getId()).getCqlString();
        }
        String cql = String.format(cqlTemplate, searchQuery.getQuery());

        // NOTE: this query means return all the name records that
        // have an entry for this source; it does NOT mean search the name
        // values for this source ONLY. I think.
        String source = searchQuery.getExtraParams().get(EXTRA_PARAM_SOURCE_FROM_PATH);
        if(source != null) {
            cql += String.format(" and local.sources = \"%s\"", source.toLowerCase());
        }

        return cql;
    }

    /**
     * Does actual work of performing a search and parsing the XML.
     * @param query
     * @return
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    @Override
    public List<Result> search(SearchQuery query) throws Exception {
        String cql = createCqlQueryString(query);
        String url = String.format("http://www.viaf.org/viaf/search?query=%s&sortKeys=holdingscount&maximumRecords=%s&httpAccept=application/xml",
                UriUtils.encodeQueryParam(cql, "UTF-8"), query.getLimit());

        HttpURLConnection conn = getConnectionFactory().createConnection(url);
        InputStream response = conn.getInputStream();

        SAXParser parser = spf.newSAXParser();
        VIAFParser viafParser = new VIAFParser(findSource(query), query);

        long start = System.currentTimeMillis();
        parser.parse(response, viafParser);
        long parseTime = System.currentTimeMillis() - start;

        try {
            response.close();
            conn.disconnect();
        } catch(IOException ioe) {
            log.error("Ignoring error from trying to close input stream and connection: " + ioe);
        }

        List<Result> results = viafParser.getResults();
        log.debug(String.format("Query: %s - parsing took %dms, got %d results",
                query.getQuery(), parseTime, results.size()));

        return results;
    }

}
