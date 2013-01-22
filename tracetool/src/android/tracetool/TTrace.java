/*
 * TTrace.java
 *
 * HomePage :  http://www.codeproject.com/csharp/TraceTool.asp
 * Download :  http://sourceforge.net/projects/tracetool/
 * See License.txt for license information
 *
 * Author : Thierry Parent
 * Version : 12.4
 */

package android.tracetool;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Properties;


import android.util.Log;

//import service.tracetool.*;   

// ----------------------------------------------------------------------
// ----------------------------------------------------------------------
// ----------------------------------------------------------------------

/*
 * * TTrace is the entry point for all traces.<p> TTrace give 3 'TraceNode' doors : Warning , Error and Debug.<p> Theses 3 doors are displayed with a special icon (all of them have the 'enabled' property set to true.<p> That class is fully static.<p>
 * @author tpa
 */

public final class TTrace 
{

   /*
    * * TTrace options
    */

   public static TTraceOptions options = new TTraceOptions();
   
   /* Android context to use to connect to service */
   //public static Context context = null ;   // TODO : replace with a call to TTrace.SetServiceMode(Context context) ;

   private static WinTrace fWinTrace;

   private static WinWatch fWinWatches;

   protected static SenderThread senderThread;

   protected static HashSet<Class<?>> nativeClasses = new HashSet<Class<?>>();

   public static String socketStatus = "Sender thread not yet started" ;

   // ----------------------------------------------------------------------

   /** no public constructor */
   private TTrace()
   {
      // no public constructor
   }

   // ----------------------------------------------------------------------
  
   /*
   public static String getServiceStatus()
   {
      if (SenderThread.tracetoolService == null)
         return "no connection" ;
      try {
         return SenderThread.tracetoolService.getStatus();
      } catch (RemoteException e) {
         return "service error" ;
      }
   }
   */
   
   // ----------------------------------------------------------------------

   /** class initialisation : create the sender thread */
   static
   {
      senderThread = new SenderThread();
      senderThread.start();

      Properties property = new Properties();
      try
      {
         // property.load(new FileInputStream("TraceTool.properties"));
         java.net.URL url = property.getClass().getResource("/TraceTool.properties"); //$NON-NLS-1$
         if (url != null)
         {
            String filename = url.getFile();
            property.load(new FileInputStream(filename));

            Enumeration<?> configFileKeys = property.propertyNames();

            while (configFileKeys.hasMoreElements())
            {
               String key = (String) configFileKeys.nextElement();
               String value = property.getProperty(key);
               boolean bValue;
               if (value.compareToIgnoreCase("true") == 0) //$NON-NLS-1$
                  bValue = true;
               else
                  bValue = false;

               if (key.compareToIgnoreCase("sendFunctions") == 0) { //$NON-NLS-1$
                  options.sendFunctions = bValue;
               } else if (key.compareToIgnoreCase("sendInherited") == 0) { //$NON-NLS-1$
                  options.sendInherited = bValue;
               } else if (key.compareToIgnoreCase("sendPrivate") == 0) { //$NON-NLS-1$
                  options.sendPrivate = bValue;
               } else if (key.compareToIgnoreCase("socketHost") == 0) { //$NON-NLS-1$
                  options.socketHost = value;
               } else if (key.compareToIgnoreCase("sendDate") == 0) { //$NON-NLS-1$
                  options.sendDate = bValue;
               } else if (key.compareToIgnoreCase("sendThreadName") == 0) { //$NON-NLS-1$
                  options.sendThreadName = bValue;
               } else if (key.compareToIgnoreCase("objectTreeDepth") == 0) { //$NON-NLS-1$
                  options.objectTreeDepth = Integer.parseInt(value);
               } else if (key.compareToIgnoreCase("DebugEnabled") == 0) { //$NON-NLS-1$
                  debug().enabled = bValue;
               } else if (key.compareToIgnoreCase("WarningEnabled") == 0) { //$NON-NLS-1$
                  warning().enabled = bValue;
               } else if (key.compareToIgnoreCase("ErrorEnabled") == 0) { //$NON-NLS-1$
                  error().enabled = bValue;
               } else if (key.compareToIgnoreCase("SendMode") == 0) { //$NON-NLS-1$
                  if (value.compareToIgnoreCase("WinMsg") == 0)  //$NON-NLS-1$
                     options.sendMode = SendMode.Socket ;      // not supported transport layer. Use socket
                  else if (value.compareToIgnoreCase("Socket") == 0)  //$NON-NLS-1$
                     options.sendMode = SendMode.Socket ; 
                  else if (value.compareToIgnoreCase("None") == 0)  //$NON-NLS-1$
                     options.sendMode = SendMode.None ;
               } else if (key.compareToIgnoreCase("SocketPort") == 0) { //$NON-NLS-1$
                  try
                  {
                     options.socketPort = Integer.parseInt(value);
                  } catch (Exception e1)
                  {
                     // eat exception
                  }
               } else if (value.compareToIgnoreCase("ToString") == 0 || value.compareToIgnoreCase("") == 0) { //$NON-NLS-1$//$NON-NLS-2$
                  // add 'Native' class to hashTable
                  try
                  {
                     Class<?> nativeClass = Class.forName(key);
                     nativeClasses.add(nativeClass);
                  } catch (ClassNotFoundException e)
                  {
                     // Class exception, it's not a correct class name
                  }
               }
            }

         }
      } catch (Exception e1)
      {
         // do nothing
      }

   }

   // ----------------------------------------------------------------------

   /**
    * The WinTrace where traces are send. <p>
    * Warning, error, debug and clearAll functions use this WinTrace.
    * @return The main trace window
    */
   public static WinTrace winTrace()
   {
      if (fWinTrace == null)
      {
         fWinTrace = new WinTrace();
      }
      return fWinTrace;
   }

   // --------------------------------------------------------------------------------

   /**
    * The WinWatch where main watches are send. <p>
    * @return The main WinWatch window
    */
   public static WinWatch watches()
   {
      if (fWinWatches == null)
      {
         fWinWatches = new WinWatch(); // protected
      }
      return fWinWatches;
   }

   // --------------------------------------------------------------------------------

   /**
    * Debug, Warning, Error are the 3 normal entry points
    * @return The Debug entry
    */
   public static TraceToSend debug()
   {
      return winTrace().debug();
   }

   // ----------------------------------------------------------------------

   /**
    * Debug, Warning, Error are the 3 normal entry points
    * @return The warning entry
    */
   public static TraceToSend warning()
   {
      return winTrace().warning();
   }

   // ----------------------------------------------------------------------

   /**
    * Debug, Warning, Error are the 3 normal entry points
    * @return the error entry
    */
   public static TraceToSend error()
   {
      return winTrace().error();
   }

   // ----------------------------------------------------------------------

   /**
    * Show or hide the trace program
    * @param isVisible True to show the viewer
    */
   public static void show(final boolean isVisible)
   {
      ArrayList<String> commandList = new ArrayList<String>();
      Utility.addCommand(commandList,TraceConst.CST_SHOW, isVisible) ;
      sendToClient(commandList);
   }

   // ----------------------------------------------------------------------

   /**
   * Set the global search criteria. You must call TTrace.winTrace().findNext() to position to the next or previous matching node
   * @param Text Text to search
   * @param Sensitive Search is case sensitive
   * @param WholeWord Match only whole word
   * @param Highlight Highlight results
   * @param SearchInAllPages Call to FindNext will search also in other traces windows if true
   */
   public static void find (final String Text, boolean Sensitive, final boolean WholeWord , final boolean Highlight, final boolean SearchInAllPages) 
   
   {
      ArrayList<String> commandList = new ArrayList<String>();
      int flags = 0 ;
      // Sensitive<<3+WholeWord<<2+highlight<<1+SearchInAllPages
      if (Sensitive)
         flags += 8;
      if (WholeWord)
         flags += 4;
      if (Highlight)
         flags += 2;
      if (SearchInAllPages)
         flags += 1;

      Utility.addCommand(commandList,TraceConst.CST_FIND_TEXT ,flags, Text);
      sendToClient(commandList);
   }
   
   // ----------------------------------------------------------------------

   /**
    * Close the viewer (not hide as show(false)
    */
   public static void closeViewer()
   {
      ArrayList<String> commandList = new ArrayList<String>();
      Utility.addCommand(commandList,TraceConst.CST_CLOSE_VIEWER);
      sendToClient(commandList);
   }

   // ----------------------------------------------------------------------
  /**
    * Clear all traces
    */
   public static void clearAll()
   {
      winTrace().clearAll();
   }

   // ----------------------------------------------------------------------

   /**
    * Close the socket. <p>
    * runFinalizersOnExit is deprecated, then call closeSocket before closing your application.
    */
   public static void closeSocket()
   {
      try
      {
         if (SenderThread.socket != null) {         
            SenderThread.socket.close();
            SenderThread.socket = null;
         } else if (SenderThread.socketUdp != null) {         
            SenderThread.socketUdp.close();
            SenderThread.socketUdp = null;
         //} else if (SenderThread.tracetoolService != null) {         
         //   TTrace.context.unbindService(SenderThread.tracetoolConnection);
         } 
      } catch (IOException e)
      {
         // no exception
      }
   }

   // ----------------------------------------------------------------------

   /**
    * flush remaining traces to the viewer
    */
   public static void flush()
   {
      // Don't add CST_FLUSH if no connection. That will block this thread 
      if (TTrace.options.sendMode == SendMode.None) { 
         return ;
      //} else if (TTrace.options.sendMode == SendMode.Service) {
      //   if (SenderThread.tracetoolService == null) 
      //      return ;
      } else if (TTrace.options.socketUDP == true) {
         if (SenderThread.socketUdp == null) 
            return ;
      } else {  // classic TCP socket
         if (SenderThread.socket == null)
            return ;
      }      
      
      Thread thread = Thread.currentThread(); // under JAVA, the thread ID don't exist, instead I use Thread name

      // use the thread name as the hastable key
      String thName = thread.getName();
      FlushStatus status = new FlushStatus();
      status.status = 1;

      // save the string object it in the threadList table.
      // the sender thread can retrieve this string using his name.
      SenderThread.threadList.put(thName, status);

      // send the CST_FLUSH command to the thread with the thread name as parameter
      ArrayList<String> commandList = new ArrayList<String>();
      Utility.addCommand(commandList,TraceConst.CST_FLUSH, thName);

      // add message to queue
      sendToClient(commandList); // thread is notified

      // now wait for the sender thread to receive the message in the queue list and call the notify method for this thread

      try
      {
         synchronized (status)
         {
            if (status.status == 1) // not yet changed by thread
            {
               synchronized (SenderThread.dataReady)
               {
                  SenderThread.dataReady.notify(); // the sender thread will wait until the thName is released
               }
               status.wait(); // release and wait for notify
            }
         }
      } catch (InterruptedException e) // Eat the exception
      {
         // System.out.println(e) ;
      }
      // remove the object when finished.
      SenderThread.threadList.remove(thName);
   }

   // ----------------------------------------------------------------------

   /**
    * Send the ArrayList to the viewer (using thread)
    * @param commandList The message list to send
    * @param winTrace the receiver window
    */
   protected static void sendToWinTraceClient(final ArrayList<String> commandList, final String winTraceId)
   {
      sendToWinTraceClient(commandList, winTraceId, null, null); // Utility.currentTime() , Thread.currentThread().getName()
   }

   // ----------------------------------------------------------------------
   /**
    * Send the ArrayList to the viewer (using thread)
    * @param commandList The message list to send
    * @param winTrace the receiver window
    * @param dateTime message time
    * @param threadName Thread name
    */
   protected static void sendToWinTraceClient(final ArrayList<String> commandList, final String winTraceId, final String dateTime, final String threadName)
   {
      // insert thread id
      if (TTrace.options.sendThreadName)
      {
         if (threadName == null)
            commandList.add(0,Utility.intToStr5(TraceConst.CST_THREAD_NAME) + Thread.currentThread().getName());
         else
            commandList.add(0,Utility.intToStr5(TraceConst.CST_THREAD_NAME) + threadName);
      }

      // add current time
      // SimpleDateFormat formatter = new SimpleDateFormat ("HH:mm:ss:SSS",SimpleDateFormat.getAvailableLocales ()[0]);
      if (dateTime == null)
         commandList.add(0,Utility.intToStr5(TraceConst.CST_MESSAGE_TIME) + Utility.currentTime());
      else
         commandList.add(0,Utility.intToStr5(TraceConst.CST_MESSAGE_TIME) + dateTime);

      // CST_USE_TREE MUST be inserted at the first position
      if (winTraceId != null && winTraceId != "") //$NON-NLS-1$
         commandList.add(0,Utility.intToStr5(TraceConst.CST_USE_TREE) + winTraceId);

      // append message to the queue list
      sendToClient(commandList);

   }

   // ----------------------------------------------------------------------

   /**
    * Send the ArrayList to the viewer (using thread)
    * @param commandList The message list to send
    * @param winWatch the receiver window
    */
   protected static void sendToWinWatchClient(final ArrayList<String> commandList, final WinWatch winWatch)
   {
      sendToWinWatchClient(commandList, winWatch, null, null); // Utility.currentTime(), Thread.currentThread().getName()
   }

   // ----------------------------------------------------------------------
   /**
    * Send the ArrayList to the viewer (using thread)
    * @param commandList The message list to send
    * @param winWatch the receiver window
    * @param dateTime message time
    * @param threadName Thread name
    */
   protected static void sendToWinWatchClient(final ArrayList<String> commandList, final WinWatch winWatch, final String dateTime, final String threadName)
   {
      // insert thread id
      if (TTrace.options.sendThreadName)
      {
         if (threadName == null)
            commandList.add(0,Utility.intToStr5(TraceConst.CST_THREAD_NAME) + Thread.currentThread().getName());
         else
            commandList.add(0,Utility.intToStr5(TraceConst.CST_THREAD_NAME) + threadName);
      }

      // add current time
      // SimpleDateFormat formatter = new SimpleDateFormat ("HH:mm:ss:SSS",SimpleDateFormat.getAvailableLocales ()[0]);
      if (dateTime == null)
         commandList.add(0,Utility.intToStr5(TraceConst.CST_MESSAGE_TIME) + Utility.currentTime());
      else
         commandList.add(0,Utility.intToStr5(TraceConst.CST_MESSAGE_TIME) + dateTime);

      // CST_USE_TREE MUST be inserted at the first position
      // if (winWatch != null && winWatch.id != null && winWatch.id != "") //$NON-NLS-1$         
         commandList.add(0,Utility.intToStr5(TraceConst.CST_WINWATCH_ID) + winWatch.id);

      // append message to the queue list
      sendToClient(commandList);

   }

   // ----------------------------------------------------------------------
   /**
    * Add the message list to the waiting queue
    * @param commandList The Message list to send
    */
   protected static void sendToClient(final ArrayList<String> commandList)
   {
      synchronized (SenderThread.msgListCriticalSection)
      {
         // add message
         SenderThread.setMessageList.add(commandList);
      }
      synchronized (SenderThread.dataReady)
      {
         SenderThread.dataReady.notify(); // the sender thread will wait until the thName is released
      }
  }
   // ----------------------------------------------------------------------

} // TTrace

// ----------------------------------------------------------------------
// ----------------------------------------------------------------------
// ----------------------------------------------------------------------

/**
 * Internal Use.<p>
 * Used to synchronize the calling thread
 */

class FlushStatus
{
   int status;
}

//----------------------------------------------------------------------

/*
 * InternalUse.<p> Send messages in the waiting queue
 */

class SenderThread extends Thread
{
   // 'Wait' Causes current thread to wait until another thread invokes the notify()} method or the notifyAll() method for this object.
   // In other words, this method behaves exactly as if it simply performs the call wait(0)
   // The current thread must own this object's monitor. The thread releases ownership of this monitor and waits until another thread
   // notifies threads waiting on this object's monitor to wake up either through a call to the notify method or the
   // notifyAll method. The thread then waits until it can re-obtain ownership of the monitor and resumes execution.

   // Create 2 messagesList.
   // All incoming traces are added in the SetMessageList.
   // The GetMessageList is empty.
   // When the thread wake up, he swap the 2 lists, exposing the empty list
   // for another incoming trace. When the thread finish sending messages,
   // the array is cleared and the thread come back to sleep for another swap

   /** Incoming Message list */
   public static ArrayList<ArrayList<String>> setMessageList = new ArrayList<ArrayList<String>>();

   /** the thread message list */
   public static ArrayList<ArrayList<String>> getMessageList = new ArrayList<ArrayList<String>>();

   /** Used by the CST_FLUSH command */
   public static Hashtable<String, FlushStatus> threadList = new Hashtable<String, FlushStatus>();

   /** Used by the CST_FLUSH command */
   public static Integer dataReady = new Integer(0);

   /** Critical section used to swap message list */
   public static String msgListCriticalSection = new String(""); //$NON-NLS-1$

   /** the different sockets to communicate with the viewer */
   protected static Socket socket;   // classic client socket 

   protected static DatagramSocket socketUdp ;  // UDP client socket
   
   //protected static ServerSocket serverSocket ;  // Server socket for USB mode
   
   /** main internal Wintrace  */
   protected static InternalWinTrace DefaultWinTrace = new InternalWinTrace() ;  // main InternalWinTrace. Id is empty;

   /** list of internal Wintrace */
   protected static ArrayList<InternalWinTrace> FormTraceList = new ArrayList<InternalWinTrace>() ;
   
   //protected static ITracetoolService tracetoolService ;     // the interface to talk to the service
   //protected static ServiceConnection tracetoolConnection ;  // object that receive connection status
   
   // ----------------------------------------------------------------------

   public boolean isSocketError = false;
   long errorTime = 0;

   // ----------------------------------------------------------------------

   /*
   public void openService()
   {
      if ( // TTrace.options.sendMode == SendMode.Service &&  
          tracetoolConnection == null) {
      
         tracetoolConnection = new ServiceConnection() 
         {
            public void onServiceConnected(ComponentName className, IBinder service) 
            {
               // This is called when the connection with the service has been established, giving us the service object we can use to
               // interact with the service.  We are communicating with our service through an IDL interface, so get a client-side
               // representation of that from the raw service object.
               //Log.i("SenderThread","Service Attached") ;    
               TTrace.socketStatus = "Tracetool service connected" ;
               tracetoolService = ITracetoolService.Stub.asInterface(service);
            }

            public void onServiceDisconnected(ComponentName className) {
               // This is called when the connection with the service has been
               // unexpectedly disconnected -- that is, its process crashed.
               TTrace.socketStatus = "Tracetool service Disconnected" ;
               //Log.i("SenderThread","Service Disconnected.");
               tracetoolService = null;      
            }
         };                  
         
         TTrace.socketStatus = "Opening tracetool service" ;
         
         ComponentName res ;
         Intent intent  ; // (TTrace.context, TracetoolService.class ) ;
         
         intent = new Intent( "service.tracetool.SocketService.ACTION" ) ;
         intent.putExtra("ACTION", "SenderThread");
         res = TTrace.context.startService(intent);
         if (res == null) {
            Log.e("SenderThread","Service not started.") ;
            TTrace.socketStatus = "Error : Tracetool service not started" ;
            return ;  // don't bind if it's not possible to start service
         } else {
            //Log.i("SenderThread","Service started " + res.getClass().toString()) ;
            TTrace.socketStatus = "Tracetool service started" ;
         }

         // Asynchronous binding. 
         boolean isOk = TTrace.context.bindService(intent,  tracetoolConnection, Context.BIND_AUTO_CREATE);
         if (isOk == false) {
            Log.e("SenderThread","Service not started. check ActivityManager log");
         } else {
            //Log.i("SenderThread","bindService ok ") ;
         }
         
         // note that tracetoolConnection.onServiceConnected is called later  
         // so that the tracetoolService object is null for now
         
         // wait max 10 sec for the service
         //Log.i("SenderThread","wait tracetoolService") ;
         TTrace.socketStatus = "Waiting tracetool service" ;
         Integer cpt = 0 ;
         while (tracetoolService == null)
         {
            //Log.i("SenderThread","loop more 500 ms") ;
            synchronized (cpt) {try {cpt.wait(500) ;} catch (InterruptedException e1) {}}
            cpt++ ;
            if (cpt > 20)
               break ;
         }
         //Log.i("SenderThread","tracetoolService ready ") ;
         TTrace.socketStatus = "tracetool service ready" ;         
      }
   }
   */
   // ----------------------------------------------------------------------
  
   /**
    * send messages to viewer
    * @see java.lang.Runnable#run()
    */
   public void run()
   {
      Thread.currentThread().setName("TraceTool thread") ;
      while (true)
      {
         // Wait for dataReady
         //--------------------------------------------
         try
         {
            synchronized (SenderThread.dataReady)
            {
               //TTrace.status = "Wait Message" ;
               SenderThread.dataReady.wait(5000); // wait message
            }
         } catch (InterruptedException e) { 
            // no exception
         }

         // if Service mode and if needed , open the service
         //--------------------------------------------------
         //openService() ;
     
         // Swap Messages List
         //--------------------------------------------
         synchronized (msgListCriticalSection)
         {
            if (setMessageList.size() == 0)
               continue;
            // swap the 2 array
            ArrayList<ArrayList<String>> temp = getMessageList; 
            getMessageList = setMessageList;
            setMessageList = temp;
            // in test mode we add the number of messages to send
            // msgListCriticalSection = msgListCriticalSection + "" + getMessageList.size() + ";" ;
         }

         // Loop messages 
         //--------------------------------------------

         
         for (int c = 0; c < getMessageList.size() ;c++)
         {
            ArrayList<String> commandList = (ArrayList<String>) getMessageList.get(c);

            String singleMessage;
            
            // Check for flush command
            //-----------------------------
            if (commandList.size() > 0)
            {
               // special case : the CST_FLUSH message is handled by the sender thread, not to be send
               singleMessage = (String) commandList.get(0);
               if (Integer.parseInt(singleMessage.substring(0, 5).trim()) == TraceConst.CST_FLUSH)
               {
                  String thName = singleMessage.substring(5, singleMessage.length());
                  Object obj = threadList.get(thName);
                  FlushStatus status = (FlushStatus) obj;
                  synchronized (status)
                  {
                     // status.v = "done" ;
                     status.status = 2;
                     status.notify();
                  }
                  continue; // get next message
               }
            }

            // check if the message contains information for local log
            //--------------------------------------------
            ParseForInternal (commandList);

            // do nothing more if sendMode is set to none
            //--------------------------------------------
            if (TTrace.options.sendMode == SendMode.None) 
               continue ;// current message is not processed. Get next message.
            

            // check previous socket error
            //--------------------------------------------
         
            // if a previous socket error occurred, don't try to send waiting messages until time out
            if (isSocketError)
            {
               long actTime = java.lang.System.currentTimeMillis();               
               if (actTime - errorTime > 1000)
                  isSocketError = false;
               else {
                  // To don't keep any traces in memory when socket is in error mode.
                  // Messages are then discarded.
                  // Only the Flush command are checked and ParseForInternal is called
                  Log.e("TT","   message is lost " + (actTime - errorTime)) ;
                  
                  
                  //try {                    
                  //   Log.i("SenderThread","wait 300 ms") ;
                  //   
                  //   synchronized (this)   // wait must be synchronized
                  //   {                     
                  //      wait(300); // wait message
                  //   }
                  //} catch (InterruptedException e) {
                  //   //  
                  //} 

                  continue;   // message is lost
               }
            }            
            
            // Generate buffer
            //--------------------------------------------

            // first count the number of chars to allocate StringBuffer in one block
            int tot = 0;
            for (int i = 0; i < commandList.size(); i++)
            {
               singleMessage = (String) commandList.get(i);
               tot += singleMessage.length() + 1;
            }
            tot++; 

            // build the buffer
            StringBuffer strBuff = new StringBuffer(tot);
            strBuff.append('\uFEFF');
            for (int i = 0; i < commandList.size(); i++)
            {
               singleMessage = (String) commandList.get(i);
               strBuff.append(singleMessage).append('\0');
            }
            strBuff.append('\0');

            String stringToSend = strBuff.toString() ;    // needed for "Service" mode
            byte[] bytes = null ;
            try
            {
               bytes = stringToSend.getBytes("UTF-16LE") ;//$NON-NLS-1$
            } catch (UnsupportedEncodingException e1) {
               continue ;
            } 

            tot = bytes.length ; // number of chars * 2
            byte[] buffToSend = new byte[5 + tot];        // needed for "socket" mode (TCP / UDP / server)

            // the WMD byte ensure the message is valid.
            buffToSend[0] = TraceConst.WMD;

            // append size
            buffToSend[1] = (byte) (tot & 0x00FF);
            buffToSend[2] = (byte) ((tot >> 8) & 0x000000FF);
            buffToSend[3] = (byte) ((tot >> 16) & 0x000000FF);
            buffToSend[4] = (byte) ((tot >> 24) & 0x000000FF);

            // append buffer
            for (int i = 0; i < bytes.length; i++)
               buffToSend [i+5] = bytes[i] ;

            OutputStream oStream ;
            /*
            if (TTrace.options.sendMode == SendMode.Service) {
                  
               try {
                  // send the string to the service. If the service is still not available, message is lost
                  if (tracetoolService != null)
                     tracetoolService.sendToViewer(stringToSend) ;            
               } catch (Exception e) {
                  // handle exception
               }
               
            } else 
            */   
            if (TTrace.options.socketUDP == true) {   // socket udp

               try
               {
                  if (socketUdp == null) {                     
                     socketUdp = new DatagramSocket();
                  }
                  InetAddress serveur = InetAddress.getByName(TTrace.options.socketHost);
                  DatagramPacket packet = new DatagramPacket(buffToSend, tot+5,serveur , TTrace.options.socketPort);
                  socketUdp.send(packet) ;
                  //lowTrace(" : OK\n") ; //$NON-NLS-1$
               } catch (Exception e)  {    // IOException              
                  TTrace.socketStatus = "Writing to UDP server socket error" ;
                  Log.e("SenderThread","Error while opening or sending message to viewer in UDP mode") ;
                  Log.e("SenderThread",e.getMessage ()) ;                  
                  //lowTrace(new StringBuffer().append(" : ").append(e.toString()).append("\n").toString()) ; //$NON-NLS-1$ //$NON-NLS-2$
                  // java.net.SocketException: The message is larger than the maximum supported by the underlying transport: Datagram send failed                  
                  // don't consider this exception as an error : no solution !
                  errorTime = java.lang.System.currentTimeMillis();
                  // message is lost
               }

            } else {  // classic TCP socket
               try
               {
                  // it's important to understand that strings are send in ASCII (1 byte per char),
                  // because it's the common denominator for all languages targeting tracetool.
                  
                  if (socket == null)
                     socket = new Socket(TTrace.options.socketHost, TTrace.options.socketPort);
                  oStream = socket.getOutputStream();
                  oStream.write(buffToSend); // use default encoding ("Cp1252" or "ISO-8859-1")  
                  //lowTrace(" : OK\n") ; 
                  //Log.i("TT", "Write") ;
               } catch (Exception e) { // IOException
                  TTrace.socketStatus = "Writing to TCP server socket error" ;
                  Log.e("TT","SenderThread : Error while opening or sending message to viewer in client Socket mode") ;
                  Log.e("TT","   " + e.getMessage ()) ;  
                  Log.e("TT","   message is lost") ;
                  isSocketError = true;
                  socket = null ;    // force to reopen the socket next time
                  errorTime = java.lang.System.currentTimeMillis();
                  
               }
            }  // classic TCP socket   
         } // next message
         
         // clear the list so the next synchronized getAll method will
         // replace the SetMessageList array by an empty array.
         getMessageList.clear();

      }  // while true
   }  // run

   // ------------------------------------------------------------------------------
   
   protected static InternalWinTrace getInternalTraceForm(final String TraceWinId, final boolean doCreate)
   {
      if (TraceWinId == null || TraceWinId .length() == 0 || TraceWinId.compareTo("_") == 0) //$NON-NLS-1$ 
         return DefaultWinTrace;   // static

      // FormTraceList is static
      for (int c = 0; c < FormTraceList.size(); c++)  
      {
         InternalWinTrace internalForm = (InternalWinTrace) FormTraceList.get(c);
         if (internalForm.Id.compareTo(TraceWinId) == 0)
            return internalForm;  
      }

      // if the trace window don't exist, create it if needed
      InternalWinTrace result = null ;
      if (doCreate)
      {
         result = new InternalWinTrace();
         result.LogFileName = ""; //$NON-NLS-1$
         result.LogFileType = 3;   // no log
         result.IsMultiColTree = false;
         FormTraceList.add(result);
         result.Id = TraceWinId;
      }
      return result ;
   }

   // ------------------------------------------------------------------------------

   protected void lowTrace(String msg)
   {
      String FileToWrite = "c:\\TracetoolInternalLog.txt";    //$NON-NLS-1$
      java.io.File file = new java.io.File(FileToWrite) ;
      
      RandomAccessFile f ;
      try 
      {      
         if (file.exists() == false)
         {
            f = new RandomAccessFile(FileToWrite, "rw"); //$NON-NLS-1$
         } else {  
            f = new RandomAccessFile(FileToWrite, "rw"); //$NON-NLS-1$
            f.seek(f.length()); 
         }
         byte[] byteBuffer = msg.getBytes("UTF-8") ; //$NON-NLS-1$ 
         
         f.write(byteBuffer) ;  // writeUTF
         f.close();
         f=null;
         file=null;
      } catch (Exception e1)
      {
         // do nothing
      }
      
   }
   
   // ------------------------------------------------------------------------------
   
   protected void ParseForInternal(ArrayList<String> commandList)
   {
      int command ;
      String commandParams ;
      InternalWinTrace TraceForm = DefaultWinTrace ; // traces are send to the master trace form by default
      // to be valid, CST_USE_TREE or CST_USE_MULTICOL_TREE or CST_WINWATCH_ID must be the first command
      if (commandList.size() > 0) // only one message
      {
         
         String msg = (String) commandList.get(0);         
         command = Integer.parseInt(msg.substring(0, 5).trim()) ;
         commandParams = msg.substring(5, msg.length());

         if (command == TraceConst.CST_USE_TREE)
            TraceForm = getInternalTraceForm(commandParams, false);
         else if (command == TraceConst.CST_WINWATCH_ID)
            return;
      }

      // stop parsing if the winForm is not registered or the winForm don't need to be saved
      // 3, Local log is disabled
      // 4, Local log enabled. No size limit.
      // 5, Local log enabled. A new file is create each day (CCYYMMDD is appended to the filename)
      if (TraceForm == null || TraceForm.LogFileType == 3)
         return;

      String LeftMsg      = "";   // Left col                                               //$NON-NLS-1$
      String RightMsg     = "";   // right col                                              //$NON-NLS-1$
      String TraceID      = "";   // the reference of the node : it's a guid                //$NON-NLS-1$
      String ThreadID     = "";   // thread id of the sender                                //$NON-NLS-1$
      String ProcessName  = "" ;  // optional : the name of the process that send traces    //$NON-NLS-1$
      int    TreeIcon     = -1;   // -1 by default, converted to 24
      String ParentId     = "";                                                             //$NON-NLS-1$
      String MessageTime  = "";                                                             //$NON-NLS-1$

      boolean   IsNewNode = false;      // check if message is a new node
      StringBuffer MemberXml = new StringBuffer();

      for (int i = 0; i < commandList.size(); i++)
      {
         String msg = (String) commandList.get(i);         
         command = Integer.parseInt(msg.substring(0, 5).trim()) ;
         commandParams = msg.substring(5, msg.length());

         switch (command)
         {
            case TraceConst.CST_WATCH_NAME:     return;  // Bypass watches
            case TraceConst.CST_MESSAGE_TIME:   MessageTime = commandParams;           break;
            case TraceConst.CST_PROCESS_NAME:   ProcessName = commandParams;           break;
            case TraceConst.CST_THREAD_NAME:    ThreadID = commandParams;              break;
            case TraceConst.CST_ICO_INDEX:      TreeIcon = Integer.parseInt(commandParams.trim()); break;
            case TraceConst.CST_TRACE_ID:       TraceID = commandParams;               break;
            case TraceConst.CST_LEFT_MSG:       LeftMsg = commandParams;               break;   // param : msg
            case TraceConst.CST_RIGHT_MSG:      RightMsg = commandParams;              break;   // param : msg

            case TraceConst.CST_NEW_NODE:
               // param1 : Parent Node
               ParentId = commandParams;
               IsNewNode = true;
               break;

            case TraceConst.CST_CREATE_MEMBER:
               MemberXml.append("<Member>");      //$NON-NLS-1$
               Utility.htmlEncode(commandParams, MemberXml);
               break;
            case TraceConst.CST_MEMBER_COL2:
               if (commandParams != "")      //$NON-NLS-1$
               {
                  MemberXml.append("<ColB>");      //$NON-NLS-1$
                  Utility.htmlEncode(commandParams, MemberXml);
                  MemberXml.append("</ColB>");      //$NON-NLS-1$
               }
               break;
            case TraceConst.CST_MEMBER_COL3:
               if (commandParams != "")      //$NON-NLS-1$
               {
                  MemberXml.append("<ColC>");      //$NON-NLS-1$
                  Utility.htmlEncode(commandParams, MemberXml);
                  MemberXml.append("</ColC>");      //$NON-NLS-1$
               }
               break;
            case TraceConst.CST_MEMBER_VIEWER_KIND:
               if (Integer.parseInt(commandParams.trim()) != TraceConst.CST_VIEWER_NONE)
                  MemberXml.append("<ViewerKind>").append(commandParams).append("</ViewerKind>");      //$NON-NLS-1$//$NON-NLS-2$
               break;
            case TraceConst.CST_ADD_MEMBER:
               MemberXml.append("</Member>");      //$NON-NLS-1$
               break;
         }  // switch
      }     // for each

      // if new node then save to log file
      if (IsNewNode == false)
         return;

      StringBuffer xml = new StringBuffer();
      xml.append("<Node");      //$NON-NLS-1$
      if (ProcessName != "") {      //$NON-NLS-1$
         xml.append(" Process=\"") ;      //$NON-NLS-1$
         Utility.htmlEncode(ProcessName,xml) ;
         xml.append("\"");      //$NON-NLS-1$
      }
      // add parent relation if not root
      if (MessageTime != ""){      //$NON-NLS-1$
         xml.append(" Time=\"") ;      //$NON-NLS-1$
         Utility.htmlEncode(MessageTime,xml) ;
         xml.append("\"");      //$NON-NLS-1$
      }
      if (ParentId != ""){      //$NON-NLS-1$
         xml.append(" Parent=\"") ;      //$NON-NLS-1$
         Utility.htmlEncode(ParentId,xml) ;
         xml.append("\"");      //$NON-NLS-1$
      }
      if (TraceID != ""){      //$NON-NLS-1$
         xml.append(" Id=\"") ;  //$NON-NLS-1$
         Utility.htmlEncode(TraceID,xml) ;      
         xml.append("\"");      //$NON-NLS-1$
      }
      if (ThreadID != ""){      //$NON-NLS-1$
         xml.append(" ThId=\"") ;      //$NON-NLS-1$
         Utility.htmlEncode(ThreadID,xml) ;
         xml.append("\"");      //$NON-NLS-1$
      }
      // don't save default
      if (TreeIcon != -1 && TreeIcon != 24)
         xml.append(" Icon=\"").append(TreeIcon).append("\"");      //$NON-NLS-1$      //$NON-NLS-2$
      xml.append(">");   // <Node ...>   //$NON-NLS-1$

      if (TraceForm.IsMultiColTree)
      {
         //<ColValue Order="2">C3</ColValue>
         String[] columns = LeftMsg.split("\t");        //$NON-NLS-1$ 
         for (int c = 0; c < columns.length; c++)
         {
            String Column = columns[c]; 
            xml.append("<ColValue Order=\"").append(c).append("\">") ;      //$NON-NLS-1$      //$NON-NLS-2$
            Utility.htmlEncode(Column,xml) ;
            xml.append("</ColValue>");      //$NON-NLS-1$
         }
      } else {
         // save the tree col1
         Utility.htmlEncode(LeftMsg,xml);
         // save the tree col 2
         if (RightMsg != "") {    //$NON-NLS-1$
            xml.append("<Col2>") ;    //$NON-NLS-1$
            Utility.htmlEncode(RightMsg,xml) ;
            xml.append("</Col2>");    //$NON-NLS-1$
         }
      }

      // append member to xml
      xml.append(MemberXml);

      xml.append("</Node>");    //$NON-NLS-1$

      if (TraceForm.LogFileName.trim().length() == 0)    
         TraceForm.LogFileName = "TraceLog.xml";    //$NON-NLS-1$

      String FileToWrite = "";    //$NON-NLS-1$
      //  0, Viewer Log is disabled.<p>
      //  1, Viewer log enabled.<p>
      //  2, Viewer log enabled. A new file is create each day (CCYYMMDD is appended to the filename)<p>
      if (TraceForm.LogFileType == 3) {            // 3, Local log disabled.
         // should not happens. Detected before parsing
         return ;
      } else if (TraceForm.LogFileType == 4) {     // 4, Local log enabled. 
         FileToWrite = TraceForm.LogFileName;
         if (TraceForm.CurrentFileNumber != 0)
         {
            // Append CurrentFileNumber Before extension            
            int pos = FileToWrite.lastIndexOf('.') ;
            if (pos == -1)  // no extension
               FileToWrite = FileToWrite + '_' + TraceForm.CurrentFileNumber + ".xml" ; //$NON-NLS-1$
             else 
               FileToWrite = FileToWrite.substring(0, pos-1)+ '_' + TraceForm.CurrentFileNumber + FileToWrite.substring(pos);
         }
       } else {                                     // 5, Local log enabled. A new file is create each day (CCYYMMDD is appended to the filename)
         String FileExt = TraceForm.LogFileName.substring(TraceForm.LogFileName.lastIndexOf('.'),TraceForm.LogFileName.length());
         StringBuffer strbBuilder = new StringBuffer();
         strbBuilder.append(TraceForm.LogFileName.substring(0, TraceForm.LogFileName.length() - FileExt.length() ));
         // append YYYYMMDD
         Calendar calendar = Calendar.getInstance() ;
         //long time = new Date ().getTime () ;
         //calendar.setTime(new Date(time)) ;

         int temp;
         strbBuilder.append (calendar.get (Calendar.YEAR));
         temp = calendar.get (Calendar.MONTH);
         if (temp < 10)
            strbBuilder.append ('0');
         strbBuilder.append (temp);
         temp = calendar.get (Calendar.DAY_OF_MONTH);
         if (temp < 10)
            strbBuilder.append ('0');
         strbBuilder.append (temp);

         // add CurrentFileNumber if <> 0
         if (TraceForm.CurrentFileNumber != 0)
            strbBuilder.append('_').append(TraceForm.CurrentFileNumber) ;
        // append file extension (XML)
         strbBuilder.append(FileExt);
         FileToWrite = strbBuilder.toString();
      }

      java.io.File file = new java.io.File(FileToWrite) ;
      
      RandomAccessFile f ;
      try 
      {      
         if (file.exists() == false)
         {
            f = new RandomAccessFile(FileToWrite, "rw"); //$NON-NLS-1$
            // insert Data node as the main node
            if (TraceForm.IsMultiColTree) {
               StringBuffer strbBuilder = new StringBuffer();
               strbBuilder.append("<MainColumn>").append(TraceForm.MainCol).append("</MainColumn>") ; //$NON-NLS-1$ //$NON-NLS-2$

               // include header in file
               String[] cols = TraceForm.TitleList.split("\t");        //$NON-NLS-1$ 
               for (int c = 0; c < cols.length; c++)
               {
                  String Column = cols[c]; 

                  if (Column.length() != 0) 
                     strbBuilder.append("<ColTitle Order=\"").append(c).append("\">").append(Column).append("</ColTitle>");  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
               }
               xml.insert(0 ,strbBuilder);
            }
            xml.insert(0,"<Data>");    //$NON-NLS-1$ 
         } else {  // append only the node
            f = new RandomAccessFile(FileToWrite, "rw"); //$NON-NLS-1$
            f.seek(f.length()-7); // override the </data> tag
         }
         xml.append("\n</Data>");  //$NON-NLS-1$ 

         // no direct conversion from StringBuffer to byte array. convert first to string then to bytes
         String strBuffer = xml.toString() ;
         byte[] byteBuffer = strBuffer.getBytes("UTF-8") ; //$NON-NLS-1$          
         f.write(byteBuffer) ;  // writeUTF
         f.close();
         f=null;
         file=null;
         
         // limit file size
         if (TraceForm.MaxLines != -1)  
         {
            TraceForm.LinesWritten++ ;
            if (TraceForm.LinesWritten >= TraceForm.MaxLines){
               TraceForm.CurrentFileNumber++ ;
               TraceForm.LinesWritten = 0 ;  // reset counter
            }
         }
       
    } catch (Exception e1)
      {
         // do nothing
      }
   }
   // ------------------------------------------------------------------------------
   // ------------------------------------------------------------------------------
}



/**
 * Internal class <p>
 * @author tpa
 */
class InternalWinTrace
{
   /**Windows id  */
   public String    Id ;
   /**Multi Column  */
   public boolean   IsMultiColTree ;
   /**main column  */
   public int       MainCol ;
   /**Titles  */
   public String    TitleList ;
   /**Log FileName  */
   public String    LogFileName ;
   /**Log FileType  
    * 0, Viewer Log is disabled.<p>
    * 1, Viewer log enabled. No size limit.<p>
    * 2, Viewer log enabled. A new file is create each day (CCYYMMDD is appended to the filename)<p>
    * 3, Local log is disabled.
    * 4, Local log enabled. No size limit.
    * 5, Local log enabled. A new file is create each day (CCYYMMDD is appended to the filename).
   */
   public int       LogFileType ;
   
   /** Max number of lines before starting a new file **/
   public int       MaxLines ;
   
   /** Current file number, when MaxLines is set **/
   public int       CurrentFileNumber ;
   
   /** Number of lines written , when MaxLines is set **/
   public int       LinesWritten ;
   
   /** Constructor */
   public InternalWinTrace()
   {
      this.Id = "";                 //$NON-NLS-1$
      this.IsMultiColTree = false;
      this.MainCol = 0;
      this.TitleList = "";          //$NON-NLS-1$
      this.LogFileName = "";        //$NON-NLS-1$
      this.LogFileType = 3;
      this.MaxLines = -1 ;
      this.CurrentFileNumber = 0 ;
      this.LinesWritten = 0 ;
   }
}

