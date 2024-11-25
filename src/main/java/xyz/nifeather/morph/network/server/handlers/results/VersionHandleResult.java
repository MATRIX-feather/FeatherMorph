package xyz.nifeather.morph.network.server.handlers.results;

public record VersionHandleResult(boolean success, int result)
{
    private static final VersionHandleResult resultFailed = new VersionHandleResult(false, -1);

    public static VersionHandleResult fail()
    {
        return resultFailed;
    }

    public static VersionHandleResult from(int input)
    {
        return new VersionHandleResult(true, input);
    }
}
