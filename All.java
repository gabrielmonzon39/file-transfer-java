public class All extends Thread {

    public static final int ROUTING = 0; 
    public static final int RECEIVER = 1; 
    public static final int FOWARDINGSERVER = 2; 
    public static final int APPSERVER = 3; 
    public static final int APP = 4;
    
    int code;

    static String args1;
    static String args2;
    static String args3;

    public static void main(String[] args) {
        try {
            args1 = args[0];
            args2 = args[1];
            args3 = args[2];

            All all1 = new All();
            all1.code = ROUTING;
            all1.start();

            All all2 = new All();
            all2.code = RECEIVER;
            all2.start();

            All all3 = new All();
            all3.code = FOWARDINGSERVER;
            all3.start();

            All all4 = new All();
            all4.code = APPSERVER;
            all4.start();

            All all5 = new All();
            all5.code = APP;
            all5.start();

        } catch (Exception e) {
            ConsoleLog.printError();
        }
    }

    public void run () {
        try {
            switch (code) {
                case ROUTING:
                    String[] args = {args1, args2, args3};
                    Routing.main(args);
                    break;
                case RECEIVER:
                    Receiver.main(null);
                    break;
                case FOWARDINGSERVER:
                    FowardingServer.main(null);
                    break;
                case APPSERVER:
                    App_Server.main(null);
                    break;
                case APP:
                    App.main(null);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            ConsoleLog.printError();
        }
    }
}
