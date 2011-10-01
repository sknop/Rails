package rails.game;import java.util.*;import org.apache.log4j.Logger;import rails.common.LocalText;import rails.common.parser.ConfigurableComponentI;import rails.common.parser.ConfigurationException;import rails.common.parser.Tag;public class CompanyManager implements CompanyManagerI, ConfigurableComponentI {    /** A List with all private companies */    private List<PrivateCompany> lPrivateCompanies =            new ArrayList<PrivateCompany>();    /** A List with all public companies */    private List<PublicCompany> lPublicCompanies =            new ArrayList<PublicCompany>();    /** A map with all private companies by name */    private Map<String, PrivateCompany> mPrivateCompanies =            new HashMap<String, PrivateCompany>();    /** A map with all public (i.e. non-private) companies by name */    private Map<String, PublicCompany> mPublicCompanies =            new HashMap<String, PublicCompany>();    /** A map of all type names to maps of companies of that type by name */    // TODO Redundant, current usage can be replaced.    private Map<String, Map<String, Company>> mCompaniesByTypeAndName =            new HashMap<String, Map<String, Company>>();    /** A list of all company types */    private List<CompanyTypeI> lCompanyTypes = new ArrayList<CompanyTypeI>();    /** A list of all start packets (usually one) */    private List<StartPacket> startPackets = new ArrayList<StartPacket>();    /** A map of all start packets, keyed by name. Default name is "Initial" */    private Map<String, StartPacket> startPacketMap        = new HashMap<String, StartPacket>();    /** A map to enable translating aliases to names */    protected Map<String, String> aliases = null;    private int numberOfPublicCompanies = 0;    protected static Logger log =            Logger.getLogger(CompanyManager.class.getPackage().getName());    protected GameManager gameManager;    /*     * NOTES: 1. we don't have a map over all companies, because some games have     * duplicate names, e.g. B&O in 1830. 2. we have both a map and a list of     * private/public companies to preserve configuration sequence while     * allowing direct access.     */    /**     * No-args constructor.     */    public CompanyManager() {    // Nothing to do here, everything happens when configured.    }    /**     * @see rails.common.parser.ConfigurableComponentI#configureFromXML(org.w3c.dom.Element)     */    public void configureFromXML(Tag tag) throws ConfigurationException {        gameManager = GameManager.getInstance();        /** A map with all company types, by type name */        // Localised here as it has no permanent use        Map<String, CompanyTypeI> mCompanyTypes              = new HashMap<String, CompanyTypeI>();                //NEW//        Map<String, Tag> typeTags = new HashMap<String, Tag>();        for (Tag compTypeTag : tag.getChildren(CompanyTypeI.ELEMENT_ID)) {            // Extract the attributes of the Component            String name =                    compTypeTag.getAttributeAsString(CompanyTypeI.NAME_TAG);            if (name == null) {                throw new ConfigurationException(                        LocalText.getText("UnnamedCompanyType"));            }            String className =                    compTypeTag.getAttributeAsString(CompanyTypeI.CLASS_TAG);            if (className == null) {                throw new ConfigurationException(LocalText.getText(                        "CompanyTypeHasNoClass", name));            }            if (mCompanyTypes.get(name) != null) {                throw new ConfigurationException(LocalText.getText(                        "CompanyTypeConfiguredTwice", name));            }            CompanyTypeI companyType = new CompanyType(name, className);            mCompanyTypes.put(name, companyType);            lCompanyTypes.add(companyType);            // Further parsing is done within CompanyType            companyType.configureFromXML(compTypeTag);            //NEW//            typeTags.put(name, compTypeTag);        }        /* Read and configure the companies */        for (Tag companyTag : tag.getChildren(Company.COMPANY_ELEMENT_ID)) {            // Extract the attributes of the Component            String name =                    companyTag.getAttributeAsString(Company.COMPANY_NAME_TAG);            if (name == null) {                throw new ConfigurationException(                        LocalText.getText("UnnamedCompany"));            }            String type =                    companyTag.getAttributeAsString(Company.COMPANY_TYPE_TAG);            if (type == null) {                throw new ConfigurationException(LocalText.getText(                        "CompanyHasNoType", name));            }            CompanyTypeI cType = mCompanyTypes.get(type);            if (cType == null) {                throw new ConfigurationException(LocalText.getText(                        "CompanyHasUnknownType", name, type ));            }            try {                //NEW//Company company = cType.createCompany(name, companyTag);                Tag typeTag = typeTags.get(type);                Company company = cType.createCompany(name, typeTag, companyTag);                /* Private or public */                if (company instanceof PrivateCompany) {                    mPrivateCompanies.put(name, (PrivateCompany) company);                    lPrivateCompanies.add((PrivateCompany) company);                } else if (company instanceof PublicCompany) {                    ((PublicCompany)company).setIndex (numberOfPublicCompanies++);                    mPublicCompanies.put(name, (PublicCompany) company);                    lPublicCompanies.add((PublicCompany) company);                }                /* By type and name */                if (!mCompaniesByTypeAndName.containsKey(type))                    mCompaniesByTypeAndName.put(type,                            new HashMap<String, Company>());                (mCompaniesByTypeAndName.get(type)).put(                        name, company);                String alias = company.getAlias();                if (alias != null) createAlias (alias, name);            } catch (Exception e) {                throw new ConfigurationException(LocalText.getText(                        "ClassCannotBeInstantiated", cType.getClassName()), e);            }        }        /* Read and configure the start packets */        List<Tag> packetTags = tag.getChildren("StartPacket");        if (packetTags != null) {            for (Tag packetTag : tag.getChildren("StartPacket")) {                // Extract the attributes of the Component                String name = packetTag.getAttributeAsString("name", StartPacket.DEFAULT_NAME);                String roundClass =                        packetTag.getAttributeAsString("roundClass");                if (roundClass == null) {                    throw new ConfigurationException(LocalText.getText(                            "StartPacketHasNoClass", name));                }                StartPacket sp = new StartPacket(name, roundClass);                startPackets.add(sp);                startPacketMap.put(name, sp);                sp.configureFromXML(packetTag);            }        }    }    // Post XML parsing initialisations    public void finishConfiguration (GameManager gameManager)    throws ConfigurationException {        for (PublicCompany comp : lPublicCompanies) {            comp.finishConfiguration(gameManager);        }        for (PrivateCompany comp : lPrivateCompanies) {            comp.finishConfiguration(gameManager);        }    }    private void createAlias (String alias, String name) {        if (aliases == null) {            aliases = new HashMap<String, String>();        }        aliases.put(alias, name);    }    public String checkAlias (String alias) {        if (aliases != null && aliases.containsKey(alias)) {            return aliases.get(alias);        } else {            return alias;        }    }    public String checkAliasInCertId (String certId) {        String[] parts = certId.split("-");        String realName = checkAlias (parts[0]);        if (!parts[0].equals(realName)) {            return realName + "-" + parts[1];        } else {            return certId;        }    }    /**     * @see rails.game.CompanyManagerI#getCompany(java.lang.String)     *     */    public PrivateCompany getPrivateCompany(String name) {        return mPrivateCompanies.get(name);    }    public PublicCompany getPublicCompany(String name) {        return mPublicCompanies.get(checkAlias(name));    }    public List<PrivateCompany> getAllPrivateCompanies() {        return lPrivateCompanies;    }    public List<PublicCompany> getAllPublicCompanies() {        return lPublicCompanies;    }    public List<CompanyTypeI> getCompanyTypes() {		return lCompanyTypes;	}	public Company getCompany(String type, String name) {        if (mCompaniesByTypeAndName.containsKey(type)) {            return (mCompaniesByTypeAndName.get(type)).get(checkAlias(name));        } else {            return null;        }    }    public void closeAllPrivates() {        if (lPrivateCompanies == null) return;        for (PrivateCompany priv : lPrivateCompanies) {            if (priv.isCloseable()) // check if private is closeable                priv.setClosed();        }    }    public List<PrivateCompany> getPrivatesOwnedByPlayers() {        List<PrivateCompany> privatesOwnedByPlayers =                new ArrayList<PrivateCompany>();        for (PrivateCompany priv : getAllPrivateCompanies()) {            if (priv.getPortfolio().getOwner() instanceof Player) {                privatesOwnedByPlayers.add(priv);            }        }        return privatesOwnedByPlayers;    }    public StartPacket getStartPacket (int index) {        return startPackets.get(index);    }    public StartPacket getStartPacket (String name) {        return startPacketMap.get(name);    }}