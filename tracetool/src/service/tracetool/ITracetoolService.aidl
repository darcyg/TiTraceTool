/*

 */

package service.tracetool;


/**
 * Tracetool service. Multiple applications can write traces to the service (running in another process).
 */
interface ITracetoolService {

    void sendToViewer(String commandList);

    int nbMessToSend();
    int nbMessSent();
    int nbMessReceived();
    String getStatus();
    void clearWaitingList();
}
