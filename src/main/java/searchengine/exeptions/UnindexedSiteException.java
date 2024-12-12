package searchengine.exeptions;

public class UnindexedSiteException extends Exception {
    private static String message = "Выбранный сайт не индексирован";
    public UnindexedSiteException() {
        super(message);
    }
}
