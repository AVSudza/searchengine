package searchengine.exeptions;

public class StopIndexingException extends Exception{
    private static String message = "Индексация остановлена пользователем";
    public StopIndexingException() {
        super(message);
    }
}
