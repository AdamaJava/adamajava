package org.qcmg.coverage;

public class NameGenerator implements org.eclipse.persistence.oxm.XMLNameTransformer {
    @Override
    public String transformTypeName(String s) {
        return s;
    }

    @Override
    public String transformElementName(String s) {
        return s;
    }

    @Override
    public String transformAttributeName(String s) {
        return s;
    }

    @Override
    // Use the unqualified class name as our root element name.
    public String transformRootElementName(String name) {
        String shortName = name.substring(name.lastIndexOf('.') + 1);
        if ("CoverageModel".equals(shortName)) {
            return "coverage";
        }
        return shortName;
    }
}
