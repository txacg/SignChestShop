package co.technius.signchestshop.util;

public class UpdateInformation
{

    private final String version;
    private final String type;

    public UpdateInformation(final String version, final String type)
    {
        this.type = type;
        this.version = version.split("[ ]+", 2)[1];
    }

    public String getType()
    {
        return type;
    }

    public String getVersion()
    {
        return version;
    }
}
