package csv.model;

public class CountryData {

    private String name;
    private String latitude;
    private String longitude;
    private String fullname;

    public String getName() {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;

        String fullname = "";
        if(name.equals("AF")) fullname = "Afghanistan";
        if(name.equals("AX")) fullname = "Aland Islands";
        if(name.equals("AL")) fullname = "Albania";
        if(name.equals("DZ")) fullname = "Algeria";
        if(name.equals("AS")) fullname = "American Samoa";
        if(name.equals("AD")) fullname = "Andorra";
        if(name.equals("AO")) fullname = "Angola";
        if(name.equals("AI")) fullname = "Anguilla";
        if(name.equals("AQ")) fullname = "Antarctica";
        if(name.equals("AG")) fullname = "Antigua and Barbuda";
        if(name.equals("AR")) fullname = "Argentina";
        if(name.equals("AM")) fullname = "Armenia";
        if(name.equals("AW")) fullname = "Aruba";
        if(name.equals("AU")) fullname = "Australia";
        if(name.equals("AT")) fullname = "Austria";
        if(name.equals("AZ")) fullname = "Azerbaijan";
        if(name.equals("BS")) fullname = "Bahamas the";
        if(name.equals("BH")) fullname = "Bahrain";
        if(name.equals("BD")) fullname = "Bangladesh";
        if(name.equals("BB")) fullname = "Barbados";
        if(name.equals("BY")) fullname = "Belarus";
        if(name.equals("BE")) fullname = "Belgium";
        if(name.equals("BZ")) fullname = "Belize";
        if(name.equals("BJ")) fullname = "Benin";
        if(name.equals("BM")) fullname = "Bermuda";
        if(name.equals("BT")) fullname = "Bhutan";
        if(name.equals("BO")) fullname = "Bolivia";
        if(name.equals("BA")) fullname = "Bosnia and Herzegovina";
        if(name.equals("BW")) fullname = "Botswana";
        if(name.equals("BV")) fullname = "Bouvet Island (Bouvetoya)";
        if(name.equals("BR")) fullname = "Brazil";
        if(name.equals("IO")) fullname = "British Indian Ocean Territory (Chagos Archipelago)";
        if(name.equals("VG")) fullname = "British Virgin Islands";
        if(name.equals("BN")) fullname = "Brunei Darussalam";
        if(name.equals("BG")) fullname = "Bulgaria";
        if(name.equals("BF")) fullname = "Burkina Faso";
        if(name.equals("BI")) fullname = "Burundi";
        if(name.equals("KH")) fullname = "Cambodia";
        if(name.equals("CM")) fullname = "Cameroon";
        if(name.equals("CA")) fullname = "Canada";
        if(name.equals("CV")) fullname = "Cape Verde";
        if(name.equals("KY")) fullname = "Cayman Islands";
        if(name.equals("CF")) fullname = "Central African Republic";
        if(name.equals("TD")) fullname = "Chad";
        if(name.equals("CL")) fullname = "Chile";
        if(name.equals("CN")) fullname = "China";
        if(name.equals("CX")) fullname = "Christmas Island";
        if(name.equals("CC")) fullname = "Cocos (Keeling) Islands";
        if(name.equals("CO")) fullname = "Colombia";
        if(name.equals("KM")) fullname = "Comoros the";
        if(name.equals("CD")) fullname = "Congo";
        if(name.equals("CG")) fullname = "Congo the";
        if(name.equals("CK")) fullname = "Cook Islands";
        if(name.equals("CR")) fullname = "Costa Rica";
        if(name.equals("CI")) fullname = "Cote d\"Ivoire";
        if(name.equals("HR")) fullname = "Croatia";
        if(name.equals("CU")) fullname = "Cuba";
        if(name.equals("CY")) fullname = "Cyprus";
        if(name.equals("CZ")) fullname = "Czech Republic";
        if(name.equals("DK")) fullname = "Denmark";
        if(name.equals("DJ")) fullname = "Djibouti";
        if(name.equals("DM")) fullname = "Dominica";
        if(name.equals("DO")) fullname = "Dominican Republic";
        if(name.equals("EC")) fullname = "Ecuador";
        if(name.equals("EG")) fullname = "Egypt";
        if(name.equals("SV")) fullname = "El Salvador";
        if(name.equals("GQ")) fullname = "Equatorial Guinea";
        if(name.equals("ER")) fullname = "Eritrea";
        if(name.equals("EE")) fullname = "Estonia";
        if(name.equals("ET")) fullname = "Ethiopia";
        if(name.equals("FO")) fullname = "Faroe Islands";
        if(name.equals("FK")) fullname = "Falkland Islands (Malvinas)";
        if(name.equals("FJ")) fullname = "Fiji the Fiji Islands";
        if(name.equals("FI")) fullname = "Finland";
        if(name.equals("FR")) fullname = "France, French Republic";
        if(name.equals("GF")) fullname = "French Guiana";
        if(name.equals("PF")) fullname = "French Polynesia";
        if(name.equals("TF")) fullname = "French Southern Territories";
        if(name.equals("GA")) fullname = "Gabon";
        if(name.equals("GM")) fullname = "Gambia the";
        if(name.equals("GE")) fullname = "Georgia";
        if(name.equals("DE")) fullname = "Germany";
        if(name.equals("GH")) fullname = "Ghana";
        if(name.equals("GI")) fullname = "Gibraltar";
        if(name.equals("GR")) fullname = "Greece";
        if(name.equals("GL")) fullname = "Greenland";
        if(name.equals("GD")) fullname = "Grenada";
        if(name.equals("GP")) fullname = "Guadeloupe";
        if(name.equals("GU")) fullname = "Guam";
        if(name.equals("GT")) fullname = "Guatemala";
        if(name.equals("GG")) fullname = "Guernsey";
        if(name.equals("GN")) fullname = "Guinea";
        if(name.equals("GW")) fullname = "Guinea-Bissau";
        if(name.equals("GY")) fullname = "Guyana";
        if(name.equals("HT")) fullname = "Haiti";
        if(name.equals("HM")) fullname = "Heard Island and McDonald Islands";
        if(name.equals("VA")) fullname = "Holy See (Vatican City State)";
        if(name.equals("HN")) fullname = "Honduras";
        if(name.equals("HK")) fullname = "Hong Kong";
        if(name.equals("HU")) fullname = "Hungary";
        if(name.equals("IS")) fullname = "Iceland";
        if(name.equals("IN")) fullname = "India";
        if(name.equals("ID")) fullname = "Indonesia";
        if(name.equals("IR")) fullname = "Iran";
        if(name.equals("IQ")) fullname = "Iraq";
        if(name.equals("IE")) fullname = "Ireland";
        if(name.equals("IM")) fullname = "Isle of Man";
        if(name.equals("IL")) fullname = "Israel";
        if(name.equals("IT")) fullname = "Italy";
        if(name.equals("JM")) fullname = "Jamaica";
        if(name.equals("JP")) fullname = "Japan";
        if(name.equals("JE")) fullname = "Jersey";
        if(name.equals("JO")) fullname = "Jordan";
        if(name.equals("KZ")) fullname = "Kazakhstan";
        if(name.equals("KE")) fullname = "Kenya";
        if(name.equals("KI")) fullname = "Kiribati";
        if(name.equals("KP")) fullname = "Korea";
        if(name.equals("KR")) fullname = "Korea";
        if(name.equals("KW")) fullname = "Kuwait";
        if(name.equals("KG")) fullname = "Kyrgyz Republic";
        if(name.equals("LA")) fullname = "Lao";
        if(name.equals("LV")) fullname = "Latvia";
        if(name.equals("LB")) fullname = "Lebanon";
        if(name.equals("LS")) fullname = "Lesotho";
        if(name.equals("LR")) fullname = "Liberia";
        if(name.equals("LY")) fullname = "Libyan Arab Jamahiriya";
        if(name.equals("LI")) fullname = "Liechtenstein";
        if(name.equals("LT")) fullname = "Lithuania";
        if(name.equals("LU")) fullname = "Luxembourg";
        if(name.equals("MO")) fullname = "Macao";
        if(name.equals("MK")) fullname = "Macedonia";
        if(name.equals("MG")) fullname = "Madagascar";
        if(name.equals("MW")) fullname = "Malawi";
        if(name.equals("MY")) fullname = "Malaysia";
        if(name.equals("MV")) fullname = "Maldives";
        if(name.equals("ML")) fullname = "Mali";
        if(name.equals("MT")) fullname = "Malta";
        if(name.equals("MH")) fullname = "Marshall Islands";
        if(name.equals("MQ")) fullname = "Martinique";
        if(name.equals("MR")) fullname = "Mauritania";
        if(name.equals("MU")) fullname = "Mauritius";
        if(name.equals("YT")) fullname = "Mayotte";
        if(name.equals("MX")) fullname = "Mexico";
        if(name.equals("FM")) fullname = "Micronesia";
        if(name.equals("MD")) fullname = "Moldova";
        if(name.equals("MC")) fullname = "Monaco";
        if(name.equals("MN")) fullname = "Mongolia";
        if(name.equals("ME")) fullname = "Montenegro";
        if(name.equals("MS")) fullname = "Montserrat";
        if(name.equals("MA")) fullname = "Morocco";
        if(name.equals("MZ")) fullname = "Mozambique";
        if(name.equals("MM")) fullname = "Myanmar";
        if(name.equals("NA")) fullname = "Namibia";
        if(name.equals("NR")) fullname = "Nauru";
        if(name.equals("NP")) fullname = "Nepal";
        if(name.equals("AN")) fullname = "Netherlands Antilles";
        if(name.equals("NL")) fullname = "Netherlands the";
        if(name.equals("NC")) fullname = "New Caledonia";
        if(name.equals("NZ")) fullname = "New Zealand";
        if(name.equals("NI")) fullname = "Nicaragua";
        if(name.equals("NE")) fullname = "Niger";
        if(name.equals("NG")) fullname = "Nigeria";
        if(name.equals("NU")) fullname = "Niue";
        if(name.equals("NF")) fullname = "Norfolk Island";
        if(name.equals("MP")) fullname = "Northern Mariana Islands";
        if(name.equals("NO")) fullname = "Norway";
        if(name.equals("OM")) fullname = "Oman";
        if(name.equals("PK")) fullname = "Pakistan";
        if(name.equals("PW")) fullname = "Palau";
        if(name.equals("PS")) fullname = "Palestinian Territory";
        if(name.equals("PA")) fullname = "Panama";
        if(name.equals("PG")) fullname = "Papua New Guinea";
        if(name.equals("PY")) fullname = "Paraguay";
        if(name.equals("PE")) fullname = "Peru";
        if(name.equals("PH")) fullname = "Philippines";
        if(name.equals("PN")) fullname = "Pitcairn Islands";
        if(name.equals("PL")) fullname = "Poland";
        if(name.equals("PT")) fullname = "Portugal, Portuguese Republic";
        if(name.equals("PR")) fullname = "Puerto Rico";
        if(name.equals("QA")) fullname = "Qatar";
        if(name.equals("RE")) fullname = "Reunion";
        if(name.equals("RO")) fullname = "Romania";
        if(name.equals("RU")) fullname = "Russian Federation";
        if(name.equals("RW")) fullname = "Rwanda";
        if(name.equals("BL")) fullname = "Saint Barthelemy";
        if(name.equals("SH")) fullname = "Saint Helena";
        if(name.equals("KN")) fullname = "Saint Kitts and Nevis";
        if(name.equals("LC")) fullname = "Saint Lucia";
        if(name.equals("MF")) fullname = "Saint Martin";
        if(name.equals("PM")) fullname = "Saint Pierre and Miquelon";
        if(name.equals("VC")) fullname = "Saint Vincent and the Grenadines";
        if(name.equals("WS")) fullname = "Samoa";
        if(name.equals("SM")) fullname = "San Marino";
        if(name.equals("ST")) fullname = "Sao Tome and Principe";
        if(name.equals("SA")) fullname = "Saudi Arabia";
        if(name.equals("SN")) fullname = "Senegal";
        if(name.equals("RS")) fullname = "Serbia";
        if(name.equals("SC")) fullname = "Seychelles";
        if(name.equals("SL")) fullname = "Sierra Leone";
        if(name.equals("SG")) fullname = "Singapore";
        if(name.equals("SK")) fullname = "Slovakia (Slovak Republic)";
        if(name.equals("SI")) fullname = "Slovenia";
        if(name.equals("SB")) fullname = "Solomon Islands";
        if(name.equals("SO")) fullname = "Somalia, Somali Republic";
        if(name.equals("ZA")) fullname = "South Africa";
        if(name.equals("GS")) fullname = "South Georgia and the South Sandwich Islands";
        if(name.equals("ES")) fullname = "Spain";
        if(name.equals("LK")) fullname = "Sri Lanka";
        if(name.equals("SD")) fullname = "Sudan";
        if(name.equals("SR")) fullname = "Suriname";
        if(name.equals("SJ")) fullname = "Svalbard & Jan Mayen Islands";
        if(name.equals("SZ")) fullname = "Swaziland";
        if(name.equals("SE")) fullname = "Sweden";
        if(name.equals("CH")) fullname = "Switzerland, Swiss Confederation";
        if(name.equals("SY")) fullname = "Syrian Arab Republic";
        if(name.equals("TW")) fullname = "Taiwan";
        if(name.equals("TJ")) fullname = "Tajikistan";
        if(name.equals("TZ")) fullname = "Tanzania";
        if(name.equals("TH")) fullname = "Thailand";
        if(name.equals("TL")) fullname = "Timor-Leste";
        if(name.equals("TG")) fullname = "Togo";
        if(name.equals("TK")) fullname = "Tokelau";
        if(name.equals("TO")) fullname = "Tonga";
        if(name.equals("TT")) fullname = "Trinidad and Tobago";
        if(name.equals("TN")) fullname = "Tunisia";
        if(name.equals("TR")) fullname = "Turkey";
        if(name.equals("TM")) fullname = "Turkmenistan";
        if(name.equals("TC")) fullname = "Turks and Caicos Islands";
        if(name.equals("TV")) fullname = "Tuvalu";
        if(name.equals("UG")) fullname = "Uganda";
        if(name.equals("UA")) fullname = "Ukraine";
        if(name.equals("AE")) fullname = "United Arab Emirates";
        if(name.equals("GB")) fullname = "United Kingdom";
        if(name.equals("US")) fullname = "United States of America";
        if(name.equals("UM")) fullname = "United States Minor Outlying Islands";
        if(name.equals("VI")) fullname = "United States Virgin Islands";
        if(name.equals("UY")) fullname = "Uruguay, Eastern Republic of";
        if(name.equals("UZ")) fullname = "Uzbekistan";
        if(name.equals("VU")) fullname = "Vanuatu";
        if(name.equals("VE")) fullname = "Venezuela";
        if(name.equals("VN")) fullname = "Vietnam";
        if(name.equals("WF")) fullname = "Wallis and Futuna";
        if(name.equals("EH")) fullname = "Western Sahara";
        if(name.equals("YE")) fullname = "Yemen";
        if(name.equals("ZM")) fullname = "Zambia";
        if(name.equals("ZW")) fullname = "Zimbabwe";

        this.setFullname(fullname);
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }
}

