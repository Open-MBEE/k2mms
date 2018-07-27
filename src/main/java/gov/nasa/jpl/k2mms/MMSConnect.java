package gov.nasa.jpl.k2mms;

import gov.nasa.jpl.ae.event.DoubleParameter;
import gov.nasa.jpl.ae.event.Expression;
import gov.nasa.jpl.ae.event.Parameter;
import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.mbee.util.Utils;
import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.Configuration;
import io.swagger.client.Pair;
import io.swagger.client.api.ElementApi;
import io.swagger.client.auth.ApiKeyAuth;
import io.swagger.client.auth.HttpBasicAuth;
import io.swagger.client.model.Element;
import io.swagger.client.model.Elements;

import java.io.Console;
import java.util.*;

public class MMSConnect {
    public static MMSConnect instance = null;
    String server;
    String ref;
    String project;
    String username;
    String password;

    LinkedHashMap<String, Parameter> kParams = new LinkedHashMap<String, Parameter>();
    LinkedHashMap<String, String> kToMMSElements = new LinkedHashMap<String, String>();
    LinkedHashMap<String, String> MMSToKElements = new LinkedHashMap<String, String>();
    /**
     * The set of element ids for which values are read from MMS instead of written to MMS.
     */
    LinkedHashSet<String> getFromMMS = new LinkedHashSet<String>();

    ApiClient client = Configuration.getDefaultApiClient();
    ElementApi api = null;

    public MMSConnect(String server, String ref, String project) {
        this(server, ref, project, null, null);
    }
    public MMSConnect(String server, String ref, String project, String username) {
        this(server, ref, project, username, null);
    }
    public MMSConnect(String server, String ref, String project, String username, String password) {
        this.server = server;
        this.project = project;
        this.ref = ref;
        this.username = username;
        this.password = password;
        if ( password == null ) {
            Pair p = login(username);
            if ( p != null ) {
                this.username = p.getName();
                this.password = p.getValue();
            }
        }
        initMMS();
        if ( instance == null ) {
            instance = this;
        }
    }

    private void initMMS() {
        if ( server == null ) {
            // FIXME -- uncomment line below and remove reference to jpl server in line below that.
            //server = defaultClient.getBasePath();
            server = "https://opencae.jpl.nasa.gov/alfresco/service";
        } else if ( !server.contains( "alfresco/service" ) ) {
            if ( !server.substring( server.length() - 1 ).equals("/") ) {
                server = server + "/";
            }
            server = server + "alfresco/service";
        }
        client.setBasePath( server );

        if( ref == null ) {
            ref = "master";
        }

        // Configure HTTP basic authorization: Basic
        HttpBasicAuth
                Basic = (HttpBasicAuth) client.getAuthentication( "Basic" );
        Basic.setUsername(username);
        Basic.setPassword(password);

        // Configure API key authorization: Ticket
        ApiKeyAuth Ticket =
                (ApiKeyAuth) client.getAuthentication( "Ticket");
        //        Ticket.setApiKey("YOUR API KEY");
        //        // Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
        //        //Ticket.setApiKeyPrefix("Token");

        api = new ElementApi();
    }

    public static Pair login(String username) {
        Console console = System.console();
        String password;
        Scanner scanner = new Scanner( System.in);
        String prompt = "username" + (username == null ? "" : " (" + username + ")") + ": ";
        System.out.print(prompt);
        String u = scanner.nextLine();
        u = u.replaceAll( "\n", "" );
        if ( username == null  || (u != null && u.length() > 0) ) {
            username = u;
        }
        if ( console != null ) {
            password = new String( console.readPassword( "password: " ) );
        } else {
            System.out.print( "password: " );
            u = scanner.nextLine();
            u = u.replaceAll( "\n", "" );
            password = u;
        }
        return new Pair(username, password);
    }

    public boolean connectIdStrings( String kElement, String mmsElement ) {
        kToMMSElements.put( kElement, mmsElement );
        MMSToKElements.put( mmsElement, kElement );
        return true;
    }

    public Element getMMSElement( String kElement ) {
        String mmsElement =  kToMMSElements.get( kElement );
        if ( mmsElement == null ) mmsElement = kElement;
        try {
            System.out.println("api.getElement( project=" + project +
                               ", ref=" + ref + ", mmsElement=" + mmsElement +
                               ", 0, true, null )");
            Elements elems =
                    api.getElement( project, ref, mmsElement, 0,
                                    false, null );
            System.out.println("api.getElement() found " +
                               elems.getElements().size() + " elements.");

            if ( elems != null && elems.getElements() != null && elems.getElements().size() > 0 ) {
                for ( Element el : elems.getElements() ) {
                    if ( el != null && el.get("id") != null &&
                         el.get("id").equals( mmsElement ) ) {
                        System.out.println("api.getElement() returning element: " + el);
                        return el;
                    }
                    System.out.println("api.getElement() found element: " + el);
                }
            }
        } catch ( ApiException e ) {
            e.printStackTrace();
        }
        return null;
    }


    public Object getPropertyValue( String kElement ) {
        String mmsElement =  kToMMSElements.get( kElement );
        if ( mmsElement == null ) mmsElement = kElement;
        Element elem = getMMSElement( kElement );
        try {
            String type = (String)elem.get( "type" );
            if ( type.equals("Property") ) {
                Map vMap = (Map)elem.get( "defaultValue" );
                if ( vMap != null ) {
                    Object valType = null;
                    try {
                        valType = vMap.get( "type" );
                    } catch ( Throwable t ) {
                    }
                    Object val = vMap.get( "value" );
                    return val;
                }
            }
        } catch ( Throwable t ) {
        }
        return null;
    }

    public boolean setParameterFromMMS( String kElement ) {
        Parameter p = kParams.get( kElement );
        if ( p == null ) return false;
        //Element e = getMMSElement( kElement );
        Object val = getPropertyValue( kElement );
        p.setValue( val );
        return true;
    }

    public boolean setMMSPropertyValue( String varName, Object value ) {
        Element elem = getMMSElement( varName );
        try {
            String type = (String)elem.get( "type" );
            if ( type.equals("Property") ) {
                Map vMap = (Map)elem.get( "defaultValue" );
                if ( vMap != null ) {
                    String valType = null;
                    try {
                        valType = (String)vMap.get( "type" );
                    } catch ( Throwable t ) {
                        Debug.breakpoint();
                    }
                    Object val = vMap.get( "value" );
                    Class<? extends Object> valClass = null;
                    if ( val != null ) {
                        valClass = val.getClass();
                    } else if ( valType != null ) {
                        valClass = getClassForMMSType( valType );
                    }
                    if ( valClass != null ) {
                        Object oo = Expression.evaluate( value, valClass, false );
                        if (oo != null ) {
                            value = oo;
                        }
                    }
                    vMap.put( "value", value );
                    Elements elements = new Elements();
                    elements.addElementsItem( elem );
                    api.postElements( project, ref, elements);
                    return true;
                }
            }
        } catch ( Throwable t ) {
            Debug.breakpoint();
        }
        return false;
    }

    public boolean setMMSPropertyFromParameter( String kElement ) {
        Parameter p = kParams.get( kElement );
        if ( p == null ) return false;
        Object o = p.getValueNoPropagate();
        return setMMSPropertyValue(kElement, o);
    }

    private Class<? extends Object> getClassForMMSType( String valType ) {
        if ( Utils.isNullOrEmpty( valType ) ) {
            return null;
        }
        if ( valType.equals( "LiteralString" ) ) {
            return String.class;
        }
        if ( valType.equals( "LiteralReal" ) ) {
            return Double.class;
        }
        if ( valType.equals( "LiteralInteger" ) ) {
            return Long.class;
        }
        return null;
    }

    public boolean connect( Parameter p, String mmsElement ) {
        return connect(p, mmsElement, false);
    }
    public boolean connect( Parameter p, String mmsElement, boolean fromMMS ) {
        String qId = p.getQualifiedId( null );
        connectIdStrings( qId, mmsElement );
        kParams.put( qId, p );
        if ( fromMMS ) {
            getFromMMS.add( qId );
            getFromMMS.add( mmsElement );
            pullFromMMS( p );
        } else {
            pushToMMS( p );
        }
        return true;
    }

    public boolean pullFromMMS(Parameter p) {
        return pullFromMMS( p.getQualifiedId( null ) );
    }
    public boolean pullFromMMS(String kElement) {
        return setParameterFromMMS( kElement );
    }
    public boolean pushToMMS(Parameter p) {
        return pushToMMS( p.getQualifiedId( null ) );
    }
    public boolean pushToMMS(String kElement) {
        return setMMSPropertyFromParameter( kElement );
    }
    public boolean sync() {
        return sync(true, true);
    }
    public boolean pullFromMMS() {
        return sync(true, false);
    }
    public boolean pushToMMS() {
        return sync(false, true);
    }
    public boolean sync(boolean pull, boolean push) {
        boolean succ = true;
        for ( Map.Entry<String, String> e : kToMMSElements.entrySet() ) {
            boolean isGetFromMMS = getFromMMS.contains( e.getKey() );
            if ( pull && isGetFromMMS ) {
                boolean s = pullFromMMS( e.getKey() );
                if ( !s ) {
                    succ = false;
                }
            } else if ( push && !isGetFromMMS ) {
                boolean s = pushToMMS( e.getKey() );
                if ( !s ) {
                    succ = false;
                }
            }
        }
        return succ;
    }

    public static void main( String[] args ) {
        String mmsServer = null;
        String mmsRef = null;
        String mmsProject = null;
        String mmsUsername = null;
        String mmsPassword = null;
        String mmsIdToPush = null;
        String mmsIdToPull = null;

        for ( int i = 0; i < args.length; ++i ) {
            String arg = args[ i ];
            if ( arg.contains( "--" ) ) {
                String a = arg.toLowerCase();
                if ( a.contains( "server" ) ) {
                    mmsServer = args[ ++i ];
                } else if ( a.endsWith( "ref" ) || a.contains( "mmsref" ) || a.contains( "refid" ) ) {
                    mmsRef = args[ ++i ];
                } else if ( a.contains( "project" ) ) {
                    mmsProject = args[ ++i ];
                } else if ( a.contains( "username" ) ) {
                    mmsUsername = args[ ++i ];
                } else if ( a.contains( "password" ) ) {
                    mmsPassword = args[ ++i ];
                } else if ( a.contains( "idtopush" ) ) {
                    mmsIdToPush = args[ ++i ];
                } else if ( a.contains( "idtopull" ) ) {
                    mmsIdToPull = args[ ++i ];
                }
            }
        }

        MMSConnect mms = new MMSConnect( mmsServer, mmsRef, mmsProject,
                                         mmsUsername, mmsPassword );
        DoubleParameter dp1 = new DoubleParameter( "from_t_s", 3.33, null );
        DoubleParameter dp2 = new DoubleParameter( "to_case100_t8_ub", 4.44, null );

        if ( mmsIdToPull == null ) mmsIdToPull = "t_s";
        if ( mmsIdToPush == null ) mmsIdToPush = "case100_t8_ub";
        mms.connect( dp1, mmsIdToPull, true );
        mms.connect( dp2, mmsIdToPush, false );

        mms.sync();

        System.out.println(dp1);
        System.out.println(dp2);
    }

}
