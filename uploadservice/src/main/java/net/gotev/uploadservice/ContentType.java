package net.gotev.uploadservice;

/**
 * Static class containing string constants for the most common
 * Internet content types.
 * It's not meant to be a complete enumeration of all the known content types,
 * so don't worry if you don't find what you're looking for.
 *
 * A complete and official list can be found here:
 * https://www.iana.org/assignments/media-types
 *
 *
 * @author gotev (Aleksandar Gotev)
 *
 */
public final class ContentType {

    /**
     * Private constructor to avoid instatiation.
     */
    private ContentType() { }

    private static final String APPLICATION = "application/";

    public static final String APPLICATION_ENVOY = APPLICATION + "envoy";
    public static final String APPLICATION_FRACTALS = APPLICATION + "fractals";
    public static final String APPLICATION_FUTURESPLASH = APPLICATION + "futuresplash";
    public static final String APPLICATION_HTA = APPLICATION + "hta";
    public static final String APPLICATION_INTERNET_PROPERTY_STREAM = APPLICATION + "internet-property-stream";
    public static final String APPLICATION_MAC_BINHEX40 = APPLICATION + "mac-binhex40";
    public static final String APPLICATION_MS_WORD = APPLICATION + "msword";
    public static final String APPLICATION_OCTET_STREAM = APPLICATION + "octet-stream";
    public static final String APPLICATION_ODA = APPLICATION + "oda";
    public static final String APPLICATION_OLESCRIPT = APPLICATION + "olescript";
    public static final String APPLICATION_PDF = APPLICATION + "pdf";
    public static final String APPLICATION_PICS_RULES = APPLICATION + "pics-rules";
    public static final String APPLICATION_PKCS10 = APPLICATION + "pkcs10";
    public static final String APPLICATION_PKIX_CRL = APPLICATION + "pkix-crl";
    public static final String APPLICATION_POSTSCRIPT = APPLICATION + "postscript";
    public static final String APPLICATION_RTF = APPLICATION + "rtf";
    public static final String APPLICATION_SETPAY = APPLICATION + "set-payment-initiation";
    public static final String APPLICATION_SETREG = APPLICATION + "set-registration-initiation";
    public static final String APPLICATION_MS_EXCEL = APPLICATION + "vnd.ms-excel";
    public static final String APPLICATION_MS_OUTLOOK = APPLICATION + "vnd.ms-outlook";
    public static final String APPLICATION_MS_PKICERTSTORE = APPLICATION + "vnd.ms-pkicertstore";
    public static final String APPLICATION_MS_PKISECCAT = APPLICATION + "vnd.ms-pkiseccat";
    public static final String APPLICATION_MS_PKISTL = APPLICATION + "vnd.ms-pkistl";
    public static final String APPLICATION_MS_POWERPOINT = APPLICATION + "vnd.ms-powerpoint";
    public static final String APPLICATION_MS_PROJECT = APPLICATION + "vnd.ms-project";
    public static final String APPLICATION_MS_WORKS = APPLICATION + "vnd.ms-works";
    public static final String APPLICATION_WINHLP = APPLICATION + "winhlp";
    public static final String APPLICATION_BCPIO = APPLICATION + "x-bcpio";
    public static final String APPLICATION_CDF = APPLICATION + "x-cdf";
    public static final String APPLICATION_Z = APPLICATION + "x-compress";
    public static final String APPLICATION_TGZ = APPLICATION + "x-compressed";
    public static final String APPLICATION_CPIO = APPLICATION + "x-cpio";
    public static final String APPLICATION_CSH = APPLICATION + "x-csh";
    public static final String APPLICATION_DIRECTOR = APPLICATION + "x-director";
    public static final String APPLICATION_DVI = APPLICATION + "x-dvi";
    public static final String APPLICATION_GTAR = APPLICATION + "x-gtar";
    public static final String APPLICATION_GZIP = APPLICATION + "x-gzip";
    public static final String APPLICATION_HDF = APPLICATION + "x-hdf";
    public static final String APPLICATION_INTERNET_SIGNUP = APPLICATION + "x-internet-signup";
    public static final String APPLICATION_IPHONE = APPLICATION + "x-iphone";
    public static final String APPLICATION_JAVASCRIPT = APPLICATION + "x-javascript";
    public static final String APPLICATION_LATEX = APPLICATION + "x-latex";
    public static final String APPLICATION_MS_ACCESS = APPLICATION + "x-msaccess";
    public static final String APPLICATION_MS_CARD_FILE = APPLICATION + "x-mscardfile";
    public static final String APPLICATION_MS_CLIP = APPLICATION + "x-msclip";
    public static final String APPLICATION_MS_DOWNLOAD = APPLICATION + "x-msdownload";
    public static final String APPLICATION_MS_MEDIAVIEW = APPLICATION + "x-msmediaview";
    public static final String APPLICATION_MS_METAFILE = APPLICATION + "x-msmetafile";
    public static final String APPLICATION_MS_MONEY = APPLICATION + "x-msmoney";
    public static final String APPLICATION_MS_PUBLISHER = APPLICATION + "x-mspublisher";
    public static final String APPLICATION_MS_SCHEDULE = APPLICATION + "x-msschedule";
    public static final String APPLICATION_MS_TERMINAL = APPLICATION + "x-msterminal";
    public static final String APPLICATION_MS_WRITE = APPLICATION + "x-mswrite";
    public static final String APPLICATION_NET_CDF = APPLICATION + "x-netcdf";
    public static final String APPLICATION_PERFMON = APPLICATION + "x-perfmon";
    public static final String APPLICATION_PKCS_12 = APPLICATION + "x-pkcs12";
    public static final String APPLICATION_PKCS_7_CERTIFICATES = APPLICATION + "x-pkcs7-certificates";
    public static final String APPLICATION_PKCS_7_CERTREQRESP = APPLICATION + "x-pkcs7-certreqresp";
    public static final String APPLICATION_PKCS_7_MIME = APPLICATION + "x-pkcs7-mime";
    public static final String APPLICATION_PKCS_7_SIGNATURE = APPLICATION + "x-pkcs7-signature";
    public static final String APPLICATION_SH = APPLICATION + "x-sh";
    public static final String APPLICATION_SHAR = APPLICATION + "x-shar";
    public static final String APPLICATION_SHOCKWAVE_FLASH = APPLICATION + "x-shockwave-flash";
    public static final String APPLICATION_STUFFIT = APPLICATION + "x-stuffit";
    public static final String APPLICATION_SV4CPIO = APPLICATION + "x-sv4cpio";
    public static final String APPLICATION_SV4CRC = APPLICATION + "x-sv4crc";
    public static final String APPLICATION_TAR = APPLICATION + "x-tar";
    public static final String APPLICATION_TCL = APPLICATION + "x-tcl";
    public static final String APPLICATION_TEX = APPLICATION + "x-tex";
    public static final String APPLICATION_TEXINFO = APPLICATION + "x-texinfo";
    public static final String APPLICATION_TROFF = APPLICATION + "x-troff";
    public static final String APPLICATION_TROFF_MAN = APPLICATION + "x-troff-man";
    public static final String APPLICATION_TROFF_ME = APPLICATION + "x-troff-me";
    public static final String APPLICATION_TROFF_MS = APPLICATION + "x-troff-ms";
    public static final String APPLICATION_USTAR = APPLICATION + "x-ustar";
    public static final String APPLICATION_WAIS_SOURCE = APPLICATION + "x-wais-source";
    public static final String APPLICATION_X509_CA_CERT = APPLICATION + "x-x509-ca-cert";
    public static final String APPLICATION_PKO = APPLICATION + "ynd.ms-pkipko";
    public static final String APPLICATION_ZIP = APPLICATION + "zip";
    public static final String APPLICATION_XML = APPLICATION + "xml";

    private static final String AUDIO = "audio/";

    public static final String AUDIO_BASIC = AUDIO + "basic";
    public static final String AUDIO_MID = AUDIO + "mid";
    public static final String AUDIO_MPEG = AUDIO + "mpeg";
    public static final String AUDIO_AIFF = AUDIO + "x-aiff";
    public static final String AUDIO_M3U = AUDIO + "x-mpegurl";
    public static final String AUDIO_REAL_AUDIO = AUDIO + "x-pn-realaudio";
    public static final String AUDIO_WAV = AUDIO + "x-wav";

    private static final String IMAGE = "image/";

    public static final String IMAGE_BMP = IMAGE + "bmp";
    public static final String IMAGE_COD = IMAGE + "cod";
    public static final String IMAGE_GIF = IMAGE + "gif";
    public static final String IMAGE_IEF = IMAGE + "ief";
    public static final String IMAGE_JPEG = IMAGE + "jpeg";
    public static final String IMAGE_PIPEG = IMAGE + "pipeg";
    public static final String IMAGE_SVG = IMAGE + "svg+xml";
    public static final String IMAGE_TIFF = IMAGE + "tiff";
    public static final String IMAGE_CMU_RASTER = IMAGE + "x-cmu-raster";
    public static final String IMAGE_CMX = IMAGE + "x-cmx";
    public static final String IMAGE_ICO = IMAGE + "x-icon";
    public static final String IMAGE_PORTABLE_ANYMAP = IMAGE + "x-portable-anymap";
    public static final String IMAGE_PORTABLE_BITMAP = IMAGE + "x-portable-bitmap";
    public static final String IMAGE_PORTABLE_GRAYMAP = IMAGE + "x-portable-graymap";
    public static final String IMAGE_PORTABLE_PIXMAP = IMAGE + "x-portable-pixmap";
    public static final String IMAGE_XRGB = IMAGE + "x-rgb";
    public static final String IMAGE_XBITMAP = IMAGE + "x-xbitmap";
    public static final String IMAGE_XPIXMAP = IMAGE + "x-xpixmap";
    public static final String IMAGE_XWINDOWDUMP = IMAGE + "x-xwindowdump";

    private static final String TEXT = "text/";

    public static final String TEXT_CSS = TEXT + "css";
    public static final String TEXT_CSV = TEXT + "csv";
    public static final String TEXT_H323 = TEXT + "h323";
    public static final String TEXT_HTML = TEXT + "html";
    public static final String TEXT_IULS = TEXT + "iuls";
    public static final String TEXT_PLAIN = TEXT + "plain";
    public static final String TEXT_RICHTEXT = TEXT + "richtext";
    public static final String TEXT_SCRIPTLET = TEXT + "scriptlet";
    public static final String TEXT_TAB_SEPARATED_VALUES = TEXT + "tab-separated-values";
    public static final String TEXT_VIEWVIEW = TEXT + "webviewhtml";
    public static final String TEXT_COMPONENT = TEXT + "x-component";
    public static final String TEXT_SETEXT = TEXT + "x-setext";
    public static final String TEXT_VCARD = TEXT + "x-vcard";
    public static final String TEXT_XML = TEXT + "xml";

    private static final String VIDEO = "video/";

    public static final String VIDEO_MPEG = VIDEO + "mpeg";
    public static final String VIDEO_QUICKTIME = VIDEO + "quicktime";
    public static final String VIDEO_LA_ASF = VIDEO + "x-la-asf";
    public static final String VIDEO_MS_ASF = VIDEO + "x-ms-asf";
    public static final String VIDEO_AVI = VIDEO + "avi";
    public static final String VIDEO_MOVIE = VIDEO + "x-sgi-movie";
}
