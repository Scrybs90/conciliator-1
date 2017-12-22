
package com.codefork.refine.viaf;

import com.codefork.refine.resources.NameType;

/**
 * Different types of names. Note that this is for the application's internal
 * use; see NameType (and the asNameType() method in this enum) for a
 * class used to format data sent to VIAF.
 */
public enum VIAFNameType {
    Person("/people/person", "Person", "Personal", "local.personalNames all \"%s\""),
    Organization("/organization/organization", "Corporate Name", "Corporate", "local.corporateNames all \"%s\""),
    Location("/location/location", "Geographic Name", "Geographic", "local.geographicNames all \"%s\""),
    // can't find better freebase ids for these two
    Book("/book/book", "Work", "UniformTitleWork", "local.uniformTitleWorks all \"%s\""),
    Edition("/book/book edition", "Expression", "UniformTitleExpression", "local.uniformTitleExpressions all \"%s\"");

    // ids are from freebase identifier ns
    private final String id;
    private final String displayName;
    private final String viafCode;
    private final String cqlString;
    private NameType nameType;

    VIAFNameType(String id, String displayName, String viafCode, String cqlString) {
       this.id = id;
       this.displayName = displayName;
       this.viafCode = viafCode;
       this.cqlString = cqlString;

       this.nameType = new NameType(getId(), getDisplayName());
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getViafCode() {
        return viafCode;
    }

    public String getCqlString() {
        return cqlString;
    }

    public NameType asNameType() {
        return nameType;
    }

    public static VIAFNameType getByViafCode(String viafCodeArg) {
        for(VIAFNameType nameType: VIAFNameType.values()) {
            if(nameType.viafCode.equals(viafCodeArg)) {
                return nameType;
            }
        }
        return null;
    }

    public static VIAFNameType getById(String idArg) {
        for(VIAFNameType nameType: VIAFNameType.values()) {
            if(nameType.id.equals(idArg)) {
                return nameType;
            }
        }
        return null;
    }

}
