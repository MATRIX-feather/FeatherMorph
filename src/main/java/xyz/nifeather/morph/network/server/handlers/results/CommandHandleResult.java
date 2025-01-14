package xyz.nifeather.morph.network.server.handlers.results;

public record CommandHandleResult(boolean success, String result)
{
    private static final CommandHandleResult resultFailed = new CommandHandleResult(false, "failed");

    public static CommandHandleResult fail()
    {
        return resultFailed;
    }

    public static CommandHandleResult from(String input)
    {
        return new CommandHandleResult(true, input);
    }
}
