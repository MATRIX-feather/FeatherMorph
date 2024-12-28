package xyz.nifeather.morph.misc;

import xiamomc.pluginbase.Messages.FormattableMessage;
import xyz.nifeather.morph.FeatherMorphMain;
import xyz.nifeather.morph.messages.CapeStrings;

public class CapeURL
{
    public static final String CHERRY = "http://textures.minecraft.net/texture/afd553b39358a24edfe3b8a9a939fa5fa4faa4d9a9c3d6af8eafb377fa05c2bb";
    public static final String VANILLA = "http://textures.minecraft.net/texture/f9a76537647989f9a0b6d001e320dac591c359e9e61a31f4ce11c88f207f0ad4";
    public static final String MIGRATOR = "http://textures.minecraft.net/texture/2340c0e03dd24a11b15a8b33c2a7e9e32abb2051b2481d0ba7defd635ca7a933";

    public static final String MINECON_2016 = "http://textures.minecraft.net/texture/e7dfea16dc83c97df01a12fabbd1216359c0cd0ea42f9999b6e97c584963e980";
    public static final String MINECON_2015 = "http://textures.minecraft.net/texture/b0cc08840700447322d953a02b965f1d65a13a603bf64b17c803c21446fe1635";
    public static final String MINECON_2013 = "http://textures.minecraft.net/texture/153b1a0dfcbae953cdeb6f2c2bf6bf79943239b1372780da44bcbb29273131da";
    public static final String MINECON_2012 = "http://textures.minecraft.net/texture/a2e8d97ec79100e90a75d369d1b3ba81273c4f82bc1b737e934eed4a854be1b6";
    public static final String MINECON_2011 = "http://textures.minecraft.net/texture/953cac8b779fe41383e675ee2b86071a71658f2180f56fbce8aa315ea70e2ed6";

    public static final String REALMS = "http://textures.minecraft.net/texture/17912790ff164b93196f08ba71d0e62129304776d0f347334f8a6eae509f8a56";

    public static final String MOJANG_CLASSIC = "http://textures.minecraft.net/texture/8f120319222a9f4a104e2f5cb97b2cda93199a2ee9e1585cb8d09d6f687cb761";
    public static final String MOJANG = "http://textures.minecraft.net/texture/5786fe99be377dfb6858859f926c4dbc995751e91cee373468c5fbf4865e7151";
    public static final String MOJANG_STUDIOS = "http://textures.minecraft.net/texture/9e507afc56359978a3eb3e32367042b853cddd0995d17d0da995662913fb00f7";

    // I'm too lazy to complete these url below
    // UwU

    public static final String TRANSLATOR = "translator";
    public static final String JP_TRANSLATOR = "jp_trans";
    public static final String CN_TRANSLATOR = "cn_trans";

    public static final String MOJIRA = "jira";
    public static final String COBALT = "cobalt";
    public static final String SCROLLS = "scrolls";
    public static final String TURTLE = "turtle";
    public static final String VALENTINE = "valentine";
    public static final String BIRTHDAY = "birthday";
    public static final String dB = "db";
    public static final String MILLIONTH = "mil";
    public static final String PRISMARINE = "prismarine";
    public static final String SNOWMAN = "snowman";
    public static final String SPADE = "spade";


    public static FormattableMessage findMatching(String url)
    {
        if (url == null || url.isEmpty())
            return new FormattableMessage(FeatherMorphMain.getInstance(), "<Nil>");

        return switch (url)
        {
            case CHERRY -> CapeStrings.cherry();
            case VANILLA -> CapeStrings.vanilla();
            case MIGRATOR -> CapeStrings.migrator();

            case MINECON_2011 -> CapeStrings.minecon2011();
            case MINECON_2012 -> CapeStrings.minecon2012();
            case MINECON_2013 -> CapeStrings.minecon2013();
            case MINECON_2015 -> CapeStrings.minecon2015();
            case MINECON_2016 -> CapeStrings.minecon2016();

            case REALMS -> CapeStrings.realmsMaker();

            case MOJANG_CLASSIC -> CapeStrings.mojangClassic();
            case MOJANG -> CapeStrings.mojang();
            case MOJANG_STUDIOS -> CapeStrings.mojangStudios();

            case TRANSLATOR -> CapeStrings.translator();
            case JP_TRANSLATOR -> CapeStrings.jpTranslator();
            case CN_TRANSLATOR -> CapeStrings.cnTranslator();

            case MOJIRA -> CapeStrings.mojira();
            case COBALT -> CapeStrings.cobalt();
            case SCROLLS -> CapeStrings.scrolls();
            case TURTLE -> CapeStrings.turtle();
            case VALENTINE -> CapeStrings.valentine();
            case BIRTHDAY -> CapeStrings.birthday();
            case dB -> CapeStrings.dbCape();
            case MILLIONTH -> CapeStrings.millionthSale();
            case PRISMARINE -> CapeStrings.prismarine();
            case SNOWMAN -> CapeStrings.julian();
            case SPADE -> CapeStrings.mrMessiah();

            default -> CapeStrings.other();
        };
    }
}
