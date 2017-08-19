package edu.buffalo.cse.cse486586.simpledynamo;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Formatter;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.StringTokenizer;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.MainThread;
import android.telephony.TelephonyManager;
import android.util.Log;

public class SimpleDynamoProvider extends ContentProvider
{
    LinkedList<String> ports;
    LinkedList<String> nodeids;
    Hashtable<String,String> nodes;
    String myNodeKey;
    static final int SERVER_PORT = 10000;
    DBProvider db;
    String myPort;
    boolean TEST=true;
    Hashtable<String,String> data;

    public boolean onCreate()
    {
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
            Log.i("not waiting","======");
            db = new DBProvider(this.getContext());

            TelephonyManager tel = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
            myPort = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
            Log.i("port in dp", myPort);


            ports = new LinkedList<String>();
            nodeids = new LinkedList<String>();
            nodes = new Hashtable<String, String>();
            data=new Hashtable<String,String>();
            myNodeKey = genHash(myPort);

            ports.add("5554");
            ports.add("5556");
            ports.add("5558");
            ports.add("5560");
            ports.add("5562");

            for (int i = 0; i < ports.size(); i++) {
                nodeids.add(genHash(ports.get(i)));
                nodes.put(genHash(ports.get(i)), ports.get(i));
            }

            Collections.sort(nodeids);

           /* Log.i("ring", nodes.get(nodeids.get(0)));
            Log.i("ring", nodes.get(nodeids.get(1)));
            Log.i("ring", nodes.get(nodeids.get(2)));
            Log.i("ring", nodes.get(nodeids.get(3)));
            Log.i("ring", nodes.get(nodeids.get(4)));
*/
            Cursor flag=db.queryDb("flag","flag");

            if(flag.getCount()==0)
            {
                Log.i("---ON CREATE--","======================FRESH START==========================");
                ContentValues cv=new ContentValues();
                cv.put("key","flag");
                cv.put("value",1);
                db.insertIntoDb(cv,"flag");
            }
            else {
                Log.i("---ON CREATE--", "======================RE START==========================");


                String rep1_port;
                int temp = nodeids.indexOf(myNodeKey);
                if (temp == nodeids.size() - 1) {
                    rep1_port = nodes.get(nodeids.get(0));
                } else {
                    rep1_port = nodes.get(nodeids.get(temp + 1));
                }

                String pre1;
                String pre2;
                int temp1 = nodeids.indexOf(genHash(myPort));
                if (temp1 == 1) {
                    pre1 = nodes.get(nodeids.get(0));
                    pre2 = nodes.get(nodeids.get(nodeids.size() - 1));
                } else if (temp1 == 0) {
                    pre1 = nodes.get(nodeids.get(nodeids.size() - 1));
                    pre2 = nodes.get(nodeids.get(nodeids.size() - 2));
                } else {
                    pre1 = nodes.get(nodeids.get(temp1 - 1));
                    pre2 = nodes.get(nodeids.get(temp1 - 2));
                }

                Log.i("replica port", rep1_port);
                Log.i("pre1 port", pre1);
                Log.i("pre2 port", pre2);


               /* String req = "rep";
                String msg = rep1_port + ",," + pre1 + ",," + pre2;
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, req);*/


                String req="test";
                String msg=rep1_port+",,"+pre1+",,"+pre2;
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, req);



            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }


        return false;
    }



    public String getTargetNode(String hashedmsg)
    {
        String target="";
        if(((hashedmsg.compareTo(nodeids.get(0)) > 0) && (hashedmsg.compareTo(nodeids.get(1)) <= 0)))
        {
            target=nodes.get(nodeids.get(1));
        }
        else if(((hashedmsg.compareTo(nodeids.get(1)) > 0) && (hashedmsg.compareTo(nodeids.get(2)) <= 0)))
        {
            target=nodes.get(nodeids.get(2));
        }
        else if(((hashedmsg.compareTo(nodeids.get(2)) > 0) && (hashedmsg.compareTo(nodeids.get(3)) <= 0)))
        {
            target=nodes.get(nodeids.get(3));
        }
        else if(((hashedmsg.compareTo(nodeids.get(3)) > 0) && (hashedmsg.compareTo(nodeids.get(4)) <= 0)))
        {
            target=nodes.get(nodeids.get(4));
        }
        else
        {
            target=nodes.get(nodeids.get(0));
        }
        return target;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs)
    {
        try
        {
            String rep1_port;
            String rep2_port;
            int temp = nodeids.indexOf(genHash(myPort));
            if (temp == nodeids.size() - 1) {
                rep1_port = nodes.get(nodeids.get(0));
                rep2_port = nodes.get(nodeids.get(1));
            } else if (temp == nodeids.size() - 2) {
                rep1_port = nodes.get(nodeids.get(temp + 1));
                rep2_port = nodes.get(nodeids.get(0));
            } else {
                rep1_port = nodes.get(nodeids.get(temp + 1));
                rep2_port = nodes.get(nodeids.get(temp + 2));
            }


            db.delete(selection);


            String msg=selection;
            String REMOTE_PORT = (Integer.parseInt(rep1_port) * 2) + "";
            String requestType="delete";
            client(msg, REMOTE_PORT, requestType);

            REMOTE_PORT = (Integer.parseInt(rep2_port) * 2) + "";
            client(msg, REMOTE_PORT, requestType);


        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        return 0;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values)
    {
        Log.e("INSERT","insert started");
        synchronized (this) {

            while(!TEST)
            {
                try {
                    Thread.sleep(1000);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }


            }
            String msgKey;
            String msgText;
            String hashedMsgKey;
            String targetport;
            try {
                msgKey = values.get("key").toString();
                msgText = values.get("value").toString();
                hashedMsgKey = genHash(msgKey);
                targetport = getTargetNode(hashedMsgKey);

                if (targetport.equals(myPort))
                {
                    Cursor c=db.queryDb(msgKey,"true");
                    ContentValues cv = new ContentValues();
                    cv.put("key", msgKey);
                    cv.put("value", msgText);
                    if(c.getCount()==0)
                    {
                        cv.put("msg_port","1");
                    }
                    else
                    {
                        int temp=Integer.parseInt(c.getString(2));
                        temp++;
                        cv.put("msg_port",temp+"");
                    }
                    //cv.put("msg_port", myPort);
                    db.insertIntoDb(cv, "true");

                    String rep1_port;
                    String rep2_port;
                    int temp = nodeids.indexOf(myNodeKey);
                    if (temp == nodeids.size() - 1) {
                        rep1_port = nodes.get(nodeids.get(0));
                        rep2_port = nodes.get(nodeids.get(1));
                    } else if (temp == nodeids.size() - 2) {
                        rep1_port = nodes.get(nodeids.get(temp + 1));
                        rep2_port = nodes.get(nodeids.get(0));
                    } else {
                        rep1_port = nodes.get(nodeids.get(temp + 1));
                        rep2_port = nodes.get(nodeids.get(temp + 2));
                    }

                    String requestType = "insert";
                    String msg = msgKey + ",," + msgText + ",," + myPort;
                    String REMOTE_PORT = (Integer.parseInt(rep1_port) * 2) + "";
                    client(msg, REMOTE_PORT, requestType);

                    REMOTE_PORT = (Integer.parseInt(rep2_port) * 2) + "";
                    client(msg, REMOTE_PORT, requestType);

                } else
                {
                    String REMOTE_PORT = (Integer.parseInt(targetport) * 2) + "";
                    String requestType = "insert";
                    String msg = msgKey + ",," + msgText + ",," + targetport;
                    client(msg, REMOTE_PORT, requestType);

                    String rep1_port;
                    String rep2_port;

                    int temp = nodeids.indexOf(genHash(targetport));
                    if (temp == nodeids.size() - 1) {
                        rep1_port = nodes.get(nodeids.get(0));
                        rep2_port = nodes.get(nodeids.get(1));
                    } else if (temp == nodeids.size() - 2) {
                        rep1_port = nodes.get(nodeids.get(temp + 1));
                        rep2_port = nodes.get(nodeids.get(0));
                    } else {
                        rep1_port = nodes.get(nodeids.get(temp + 1));
                        rep2_port = nodes.get(nodeids.get(temp + 2));
                    }


                    if (rep1_port.equals(myPort)) {
                        ContentValues cv = new ContentValues();
                        cv.put("key", msgKey);
                        cv.put("value", msgText);
                        Cursor c=db.queryDb(msgKey,"true");
                        if(c.getCount()==0)
                        {
                            cv.put("msg_port","1");
                        }
                        else
                        {
                            int t=Integer.parseInt(c.getString(2));
                            t++;
                            cv.put("msg_port",t+"");
                        }
                        //cv.put("msg_port", targetport);
                        db.insertIntoDb(cv, "true");

                    }
                    else
                    {
                        REMOTE_PORT = (Integer.parseInt(rep1_port) * 2) + "";
                        client(msg, REMOTE_PORT, requestType);

                    }

                    if (rep2_port.equals(myPort)) {
                        ContentValues cv = new ContentValues();
                        cv.put("key", msgKey);
                        cv.put("value", msgText);
                        Cursor c=db.queryDb(msgKey,"true");
                        if(c.getCount()==0)
                        {
                            cv.put("msg_port","1");
                        }
                        else
                        {
                            int t=Integer.parseInt(c.getString(2));
                            t++;
                            cv.put("msg_port",t+"");
                        }
                        //cv.put("msg_port", targetport);

                        db.insertIntoDb(cv, "true");
                    }
                    else
                    {
                        REMOTE_PORT = (Integer.parseInt(rep2_port) * 2) + "";
                        client(msg, REMOTE_PORT, requestType);
                    }

                }


            } catch (Exception e) {
                e.printStackTrace();
























            }
            Log.e("INSERT","insert completed");
            return null;
        }

    }



    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        //synchronized (this) {


        while(!TEST)
        {
            try {
                Thread.sleep(1000);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }


        }


        Cursor c = null;
        String hashedkey = "";
        String targetport;
        if (selection.equals("@"))
        {
            Log.e("@ QUERY CALLED","CLIENT SIDE");
            try {
                c = db.queryDb("*", "true");
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.e("@ QUERY COMPLETED","CLIENT SIDE");
        }
        else if (selection.equals("*"))
        {
            Log.e("STAR QUERY CALLED","CLIENT SIDE");
            String cols[]={"key","value"};
            MatrixCursor mc=new MatrixCursor(cols);

            for (int i = 0; i < ports.size(); i++)
            {
                targetport = ports.get(i);
                if (targetport.equals(myPort))
                {
                    try
                    {
                        c = db.queryDb("*", "true");
                        if (c.getCount() == 0)
                        {

                        } else
                        {
                            for (int j = 0; j < c.getCount(); j++)
                            {
                                String key = c.getString(0);
                                String value = c.getString(1);
                                String row[]={key,value};
                                mc.addRow(row);
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
                else
                {
                    try
                    {
                        OutputStream osc;
                        PrintWriter pwc;
                        InputStream isc;
                        InputStreamReader isrc;
                        BufferedReader brc;
                        String remotePort = (Integer.parseInt(targetport) * 2) + "";
                        String req = "star";
                        String msg = req + ",," + selection + "\n";

                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                (Integer.parseInt(remotePort)));
                        osc = socket.getOutputStream();
                        pwc = new PrintWriter(osc);
                        pwc.write(msg);
                        pwc.flush();
                        isc = socket.getInputStream();
                        isrc = new InputStreamReader(isc);
                        brc = new BufferedReader(isrc);
                        String x = brc.readLine();
                        if (!(x.equals("")))
                        {
                            pwc.close();
                            brc.close();
                            socket.close();
                        }

                        String res;

                        if (x.equals("blank"))
                        {
                        }
                        else
                        {
                            res = x;
                            StringTokenizer st = new StringTokenizer(res, ",");
                            while (st.hasMoreTokens())
                            {
                                String k = st.nextToken();
                                String v = st.nextToken();
                                String row[]={k,v};
                                mc.addRow(row);
                            }
                        }
                    } catch (Exception e) {

                        Log.e("GOT STAR EXCEPTION", "AKAAKAKAKKAKAAKAKAKKAAKAAAAAAAAAAAAAAAAAAAAAAAKKAKAKAKAKAKAKKAAAAAAAAAAAAAAAAAKAK");

                        e.printStackTrace();
                        continue;
                    }
                }
            }

            try
            {
                c=mc;
            } catch (Exception e)
            {
                e.printStackTrace();
            }

            Log.e("@ QUERY COMPLETED","CLIENT SIDE");
        }







        else
        {




            String rep1_port="";
            String rep2_port="";
            String key="";

            LinkedList<String> val=new LinkedList<String>();
            LinkedList<String> count=new LinkedList<String>();
            Hashtable<String,Integer> ht=new Hashtable<String, Integer>();



            Log.e("NORMAL QUERY CALLED","CLIENT SIDE");
            try {
                hashedkey = genHash(selection);
            } catch (Exception e) {
                e.printStackTrace();
            }
            targetport = getTargetNode(hashedkey);

            try
            {

                int temp = nodeids.indexOf(genHash(targetport));
                if (temp == nodeids.size() - 1) {
                    rep1_port = nodes.get(nodeids.get(0));
                    rep2_port = nodes.get(nodeids.get(1));
                } else if (temp == nodeids.size() - 2) {
                    rep1_port = nodes.get(nodeids.get(temp + 1));
                    rep2_port = nodes.get(nodeids.get(0));
                } else {
                    rep1_port = nodes.get(nodeids.get(temp + 1));
                    rep2_port = nodes.get(nodeids.get(temp + 2));
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }






            if (targetport.equals(myPort))
            {
                try
                {
                    c = db.queryDb(selection, "true");
                    val.add(c.getString(1));
                    String v1=c.getString(1);
                    int version=Integer.parseInt(c.getString(2));
                    count.add(version+"");
                    ht.put(v1,version);

                    String replicas[]=new String[]{rep1_port,rep2_port};

                    for(int i=0;i<replicas.length;i++)
                    {
                        try {
                            OutputStream osc;
                            PrintWriter pwc;
                            InputStream isc;
                            InputStreamReader isrc;
                            BufferedReader brc;
                            String remotePort = (Integer.parseInt(replicas[i]) * 2) + "";
                            String req = "query";
                            String msg = req + ",," + selection + "\n";

                            Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                    (Integer.parseInt(remotePort)));
                            osc = socket.getOutputStream();
                            pwc = new PrintWriter(osc);
                            pwc.write(msg);
                            pwc.flush();

                            Log.i("CLIENT - query sent", replicas[i] + "," + selection);
                            isc = socket.getInputStream();
                            isrc = new InputStreamReader(isc);
                            brc = new BufferedReader(isrc);
                            String x = brc.readLine();
                            if (!(x.equals("blank"))) {
                                pwc.close();
                                brc.close();
                                socket.close();
                                int a = x.indexOf(",,");
                                String k = x.substring(0, a);
                                String temp = x.substring(a + 2, x.length());
                                a = temp.indexOf(",,");
                                String v = temp.substring(0, a);
                                int ver=Integer.parseInt(temp.substring(a + 2, temp.length()));
                                key=k;
                                val.add(v);
                                count.add(ver+"");
                                ht.put(v,ver);
                                    /*String row[] = new String[]{k, v};
                                    MatrixCursor mc = new MatrixCursor(new String[]{"key", "value"});
                                    mc.addRow(row);
                                    c = mc;*/

                            }
                        }
                        catch(Exception e1)
                        {
                            e1.printStackTrace();
                        }
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                String ans="";
                if(ht.size()==2)
                {
                    Set s=ht.keySet();
                    Iterator<String> it=s.iterator();
                    while(it.hasNext())
                    {
                        String a=it.next();
                        String b=it.next();
                        if(ht.get(a)>ht.get(b))
                        {
                            ans=a;
                        }
                        else
                        {
                            ans=b;
                        }

                    }
                }
                else
                {
                    ans=val.get(0);
                }




/*
                    if(val.get(0).equals(val.get(1)))
                    {
                        ans=val.get(0);
                    }
                    else if(val.get(1).equals(val.get(2)))
                    {
                        ans=val.get(1);
                    }
                    else
                    {
                        ans=val.get(0);
                    }*/

                  /*  if(val.size()==3)
                    {
                        if(ht.get(val.get(0))>=ht.get(val.get(1)))
                        {
                            if(ht.get(val.get(0))>=ht.get(val.get(2)))
                            {
                                ans=val.get(0);
                            }
                            else
                            {
                                ans=val.get(2);
                            }
                        }
                        else
                        {
                            if(ht.get(val.get(1))>=ht.get(val.get(2)))
                            {
                                ans=val.get(1);
                            }
                            else
                            {
                                ans=val.get(2);
                            }
                        }
                    }
                    else
                    {
                        if(ht.get(val.get(0))>=ht.get(val.get(1)))
                            ans=val.get(0);
                        else
                            ans=val.get(1);
                    }*/




                //ans=val.get(0);
                for(int i=0;i<val.size();i++)
                {

                    Log.e("VALUES","/////////"+val.get(i)+"///////"+count.get(i)+"/////////////////////////");

                }
                Log.e("HT","//////////////////////////"+ht.toString()+"////////////////////////////////");
                Log.e("MAJORITY","//////////////////////////"+ans+"////////////////////////////////");

                String row[] = new String[]{key, ans};
                MatrixCursor mc = new MatrixCursor(new String[]{"key", "value"});
                mc.addRow(row);
                c = mc;




            }





            else {
                try {
                    OutputStream osc;
                    PrintWriter pwc;
                    InputStream isc;
                    InputStreamReader isrc;
                    BufferedReader brc;
                    String remotePort = (Integer.parseInt(targetport) * 2) + "";
                    String req = "query";
                    String msg = req + ",," + selection + "\n";

                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            (Integer.parseInt(remotePort)));
                    osc = socket.getOutputStream();
                    pwc = new PrintWriter(osc);
                    pwc.write(msg);
                    pwc.flush();

                    Log.i("CLIENT - query sent", targetport + "," + selection);
                    isc = socket.getInputStream();
                    isrc = new InputStreamReader(isc);
                    brc = new BufferedReader(isrc);
                    String x = brc.readLine();
                    if (!(x.equals("blank"))) {
                        pwc.close();
                        brc.close();
                        socket.close();
                        int a = x.indexOf(",,");
                        String k = x.substring(0, a);
                        String temp = x.substring(a + 2, x.length());
                        a = temp.indexOf(",,");
                        String v = temp.substring(0, a);
                        int ver=Integer.parseInt(temp.substring(a + 2, temp.length()));
                        key=k;
                        val.add(v);
                        count.add(ver+"");
                        ht.put(v,ver);


                            /*String row[] = new String[]{k, v};
                            MatrixCursor mc = new MatrixCursor(new String[]{"key", "value"});
                            mc.addRow(row);
                            c = mc;*/
                    }
                } catch (Exception e) {
                    Log.i("NORMAL QUERY EXCEPTION", "AKAAKAKAKKAKAAKAKAKKAAKAAAAAAAAAAAAAAAAAAAAAAAKKAKAKAKAKAKAKKAAAAAAAAAAAAAAAAAKAK");
                    e.printStackTrace();

                       /* Log.e("NORMAL QUERY TO REP","CLIENT SIDE");
                        try
                        {
                            OutputStream osc;
                            PrintWriter pwc;
                            InputStream isc;
                            InputStreamReader isrc;
                            BufferedReader brc;

                          *//*  String rep1_port;
                            int temp=nodeids.indexOf(genHash(targetport));
                            if(temp==nodeids.size()-1)
                            {
                                rep1_port=nodes.get(nodeids.get(0));
                            }
                            else
                            {
                                rep1_port=nodes.get(nodeids.get(temp+1));
                            }*//*


                            String remotePort = (Integer.parseInt(rep1_port) * 2) + "";
                            String req = "query";
                            String msg = req + ",," + selection + "\n";

                            Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                    (Integer.parseInt(remotePort)));
                            osc = socket.getOutputStream();
                            pwc = new PrintWriter(osc);
                            pwc.write(msg);
                            pwc.flush();
                            Log.i("CLIENT - query sent",targetport+","+selection );
                            isc = socket.getInputStream();
                            isrc = new InputStreamReader(isc);
                            brc = new BufferedReader(isrc);
                            String x = brc.readLine();
                            if (!(x.equals("blank"))) {
                                pwc.close();
                                brc.close();
                                socket.close();
                                int a = x.indexOf(",,");
                                String k = x.substring(0, a);
                                String v = x.substring(a + 2, x.length());
                                String row[]=new String[]{k,v};
                                MatrixCursor mc=new MatrixCursor(new String[]{"key","value"});
                                mc.addRow(row);
                                c = mc;

                            }

                        }
                        catch (Exception e2)
                        {
                            e2.printStackTrace();
                        }*/
                }


                String replicas[] = new String[]{rep1_port, rep2_port};

                for (int i = 0; i < replicas.length; i++) {
                    try {
                        OutputStream osc;
                        PrintWriter pwc;
                        InputStream isc;
                        InputStreamReader isrc;
                        BufferedReader brc;
                        String remotePort = (Integer.parseInt(replicas[i]) * 2) + "";
                        String req = "query";
                        String msg = req + ",," + selection + "\n";

                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                (Integer.parseInt(remotePort)));
                        osc = socket.getOutputStream();
                        pwc = new PrintWriter(osc);
                        pwc.write(msg);
                        pwc.flush();

                        Log.i("CLIENT - query sent", replicas[i] + "," + selection);
                        isc = socket.getInputStream();
                        isrc = new InputStreamReader(isc);
                        brc = new BufferedReader(isrc);
                        String x = brc.readLine();
                        if (!(x.equals("blank"))) {
                            pwc.close();
                            brc.close();
                            socket.close();
                            int a = x.indexOf(",,");
                            String k = x.substring(0, a);
                            String temp = x.substring(a + 2, x.length());
                            a = temp.indexOf(",,");
                            String v = temp.substring(0, a);
                            int ver=Integer.parseInt(temp.substring(a + 2, temp.length()));
                            key=k;
                            val.add(v);
                            count.add(ver+"");
                            ht.put(v,ver);
                                /*String row[] = new String[]{k, v};
                                MatrixCursor mc = new MatrixCursor(new String[]{"key", "value"});
                                mc.addRow(row);
                                c = mc;*/

                        }
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }


                String ans="";
                if(ht.size()==2)
                {
                    Set s=ht.keySet();
                    Iterator<String> it=s.iterator();
                    while(it.hasNext())
                    {
                        String a=it.next();
                        String b=it.next();
                        if(ht.get(a)>ht.get(b))
                        {
                            ans=a;
                        }
                        else
                        {
                            ans=b;
                        }

                    }
                }
                else
                {
                    ans=val.get(0);
                }

                   /* if(val.get(0).equals(val.get(1)))
                    {
                        ans=val.get(0);
                    }
                    else if(val.get(1).equals(val.get(2)))
                    {
                        ans=val.get(1);
                    }
                    else
                    {
                        ans=val.get(0);
                    }*/

                  /*  if(val.size()==3)
                    {
                        if(ht.get(val.get(0))>=ht.get(val.get(1)))
                        {
                            if(ht.get(val.get(0))>=ht.get(val.get(2)))
                            {
                                ans=val.get(0);
                            }
                            else
                            {
                                ans=val.get(2);
                            }
                        }
                        else
                        {
                            if(ht.get(val.get(1))>=ht.get(val.get(2)))
                            {
                                ans=val.get(1);
                            }
                            else
                            {
                                ans=val.get(2);
                            }
                        }
                    }
                    else
                    {
                        if(ht.get(val.get(0))>=ht.get(val.get(1)))
                            ans=val.get(0);
                        else
                            ans=val.get(1);
                    }

*/

                //ans=val.get(0);
                for(int i=0;i<val.size();i++)
                {

                    Log.e("VALUES","/////////"+val.get(i)+"///////"+count.get(i)+"/////////////////////////");

                }
                Log.e("HT","//////////////////////////"+ht.toString()+"////////////////////////////////");
                Log.e("MAJORITY","//////////////////////////"+ans+"////////////////////////////////");
                String row[] = new String[]{key, ans};
                MatrixCursor mc = new MatrixCursor(new String[]{"key","value"});
                mc.addRow(row);
                c = mc;





            }
            Log.e("NORMAL QUERY COMPLETED","CLIENT SIDE");
        }

        return c;
        //}
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }





    public int client(String msg1, String port, String req)
    {
        OutputStream osc;
        PrintWriter pwc;
        InputStream isc;
        InputStreamReader isrc;
        BufferedReader brc;
        Socket socket;
        String remotePort="";
        String reqType="";
        String msg;
        String ackc;

        Log.i("-----------------------","-----------------------------");
        Log.i("---","----------------CLIENT--------------------------");
        Log.i("-----------------------","-----------------------------");
        try {
            remotePort = port;
            reqType=req;
            msg=msg1;
            Log.i("CLIENT",(Integer.parseInt(remotePort)/2)+"");

            socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                    Integer.parseInt(remotePort));

            String msgToSend=reqType+",,"+msg+"\n";
            ackc="PA1_OK";
            osc=socket.getOutputStream();
            pwc=new PrintWriter(osc);
            Log.i("CLIENT",msgToSend);
            pwc.write(msgToSend);
            pwc.flush();
            isc = socket.getInputStream();
            isrc=new InputStreamReader(isc);
            brc = new BufferedReader(isrc);
            String x=brc.readLine();
            if (!(x.equals(""))) {
                if (x.equals(ackc)) {
                    Log.i("CLIENT", "ack from server recieved");
                    pwc.close();
                    brc.close();
                    socket.close();
                }
            }
        }
        catch (Exception e1)
        {
            Log.e("INSERT EXCEPTION", "**************NULL POINTER OCCURED**************");
            e1.printStackTrace();


            Log.e("resend",reqType);
            if((reqType.equals("insert"))||(reqType.equals("replica")))
            {
                try
                {
                    String rep1_port;
                    int temp=nodeids.indexOf(genHash((Integer.parseInt(remotePort)/2)+""));
                    if(temp==nodeids.size()-1)
                    {
                        rep1_port=nodes.get(nodeids.get(0));
                    }
                    else
                    {
                        rep1_port=nodes.get(nodeids.get(temp+1));
                    }


                    client(msg1,(Integer.parseInt(rep1_port)*2)+"","hash");

                }
                catch(Exception e2)
                {
                    e2.printStackTrace();
                }
            }
            else
            {
                Log.e("DELETE EXCEPTION", "**************NULL POINTER DELETE**************");
            }

            e1.printStackTrace();
            Log.e("INSERT EXCEPTION", "**************NULL POINTER DONE**************");


        }
        Log.i(" client reached here", "*******************************************");
        return 0;


    }












    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {
        Socket s1;
        InputStream iss;
        OutputStream oss;
        PrintWriter pws;
        InputStreamReader isrs;
        BufferedReader brs;
        String acks = "PA1_OK";
        String string_msg;

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];

            Log.i("-----------------------", "-----------------------------");
            Log.i("---", "--------------------------SERVER--------------------------");
            Log.i("-----------------------", "-----------------------------");
            try {
                Log.e("SERVER",TEST+"");
                //Thread.sleep(15000);
                //notifyAll();

                while (true) {
                    s1 = serverSocket.accept();




                    synchronized (this) {


                        Log.i("SERVER", "reached1");
                        iss = s1.getInputStream();
                        Log.i("SERVER", "reached2");
                        isrs = new InputStreamReader(iss);
                        Log.i("SERVER", "reached3");
                        brs = new BufferedReader(isrs);
                        Log.i("SERVER", "reached4");
                        string_msg = brs.readLine();
                        Log.i("SERVER", string_msg);

                        String reqType;
                        int a = string_msg.indexOf(",,");
                        reqType = string_msg.substring(0, a);


                        Log.i("SERVER - req", reqType);


                        if (reqType.equals("insert")) {

                            Log.i("SERVER", "reached insert");
                            String msgText;
                            String msgKey;
                            String p;
                            String temp = string_msg.substring(a + 2, string_msg.length());
                            a = temp.indexOf(",,");
                            msgKey = temp.substring(0, a);
                            String temp1 = temp.substring(a + 2, temp.length());
                            a = temp1.indexOf(",,");
                            msgText = temp1.substring(0, a);
                            p = temp1.substring(a + 2, temp1.length());


                            Log.i("SERVER - key", msgKey);
                            Log.i("SERVER - text", msgText);

                            Cursor c=db.queryDb(msgKey,"true");

                            ContentValues cv = new ContentValues();
                            cv.put("key", msgKey);
                            cv.put("value", msgText);

                            if(c.getCount()==0)
                            {
                                cv.put("msg_port","1");
                            }
                            else
                            {
                                int t=Integer.parseInt(c.getString(2));
                                t++;
                                cv.put("msg_port",t+"");
                            }


                            //cv.put("msg_port", p);
                            db.insertIntoDb(cv, "true");

                            oss = s1.getOutputStream();
                            pws = new PrintWriter(oss);
                            pws.write(acks + "\n");
                            pws.flush();
                            Log.i("SERVER", "reached5");
                            pws.close();
                            brs.close();
                            s1.close();


                        }




                        else if (reqType.equals("query")) {
                            Log.i("SERVER", "reached query");

                            String sel = string_msg.substring(a + 2, string_msg.length());
                            Cursor c;
                            c = db.queryDb(sel, "true");
                            if (c.getCount() == 0) {
                                String msg = "blank\n";
                                Log.i("in query\\\\\\\\\\", msg);
                                oss = s1.getOutputStream();
                                pws = new PrintWriter(oss);
                                pws.write(msg);
                                pws.flush();
                                Log.i("SERVER", "reached5");
                                pws.close();
                                brs.close();
                                s1.close();

                            } else {
                                String value = c.getString(1);
                                String key = c.getString(0);
                                String version = c.getString(2);
                                String msg = key + ",," + value + ",," +version+ "\n";
                                oss = s1.getOutputStream();
                                pws = new PrintWriter(oss);
                                Log.i("in query\\\\\\\\\\", msg);
                                pws.write(msg);
                                pws.flush();
                                Log.i("SERVER", "reached5");
                                pws.close();
                                brs.close();
                                s1.close();

                            }


                        }





                        else if (reqType.equals("star")) {
                            Log.i("SERVER", "reached star");


                            Cursor c;
                            c = db.queryDb("*", "true");

                            String msg;
                            if (c.getCount() == 0) {
                                msg = "blank";

                            } else {
                                String key = c.getString(0);
                                String value = c.getString(1);
                                msg = key + "," + value;
                                c.moveToNext();
                                for (int i = 0; i < c.getCount() - 1; i++) {
                                    value = c.getString(1);
                                    key = c.getString(0);
                                    msg = msg + "," + key + "," + value;
                                    c.moveToNext();
                                }
                            }

                            Log.i("server-star", msg);
                            oss = s1.getOutputStream();
                            pws = new PrintWriter(oss);
                            pws.write(msg + "\n");
                            pws.flush();
                            Log.i("SERVER", "reached5");
                            pws.close();
                            brs.close();
                            s1.close();


                        }



                        else if (reqType.equals("delete")) {
                            String key = string_msg.substring(a + 2, string_msg.length());
                            db.delete(key);

                            oss = s1.getOutputStream();
                            pws = new PrintWriter(oss);
                            pws.write(acks + "\n");
                            pws.flush();
                            Log.i("SERVER", "reached5");
                            pws.close();
                            brs.close();
                            s1.close();


                        }





                        else if (reqType.equals("rep")) {

                            Log.i("..", "...............................................");
                            Log.i("...........", "SERVER RECOVERY REP.............................");
                            Log.i("..", "...............................................");

                            String temp = string_msg.substring(a + 2, string_msg.length());
                            int b = temp.indexOf(",,");
                            String port1 = temp.substring(0, b);
                            String port2 = temp.substring(b + 2, temp.length());

                            String sel = "rep";

                            Cursor c;
                            //c = db.queryDb(sel, port1+",,"+port2);
                            String s2[]={"key","value","msg_port"};
                            c=db.getReadableDatabase().query("original",s2,"(msg_port="+port1+")OR(msg_port="+port2+")",null,null,null,null,null);
                            c.moveToFirst();
                            Log.i("key",(c.getCount()+""));

                            String msg;
                            if (c.getCount() == 0) {
                                msg = "";

                            } else {
                                String key = c.getString(0);
                                String value = c.getString(1);
                                String port = c.getString(2);
                                msg = key + "," + value + "," + port;
                                c.moveToNext();
                                for (int i = 0; i < c.getCount() - 1; i++) {

                                    key = c.getString(0);
                                    value = c.getString(1);
                                    port = c.getString(2);
                                    msg = msg + "," + key + "," + value + "," + port;
                                    c.moveToNext();
                                }
                            }


                            Log.i("server-recovery-rep", msg);
                            oss = s1.getOutputStream();
                            pws = new PrintWriter(oss);
                            pws.write(msg + "\n");
                            pws.flush();
                            Log.i("SERVER", "reached5");
                            pws.close();
                            brs.close();
                            s1.close();


                        }








                        else if (reqType.equals("pre1")) {

                            Log.i("..", "...............................................");
                            Log.i("...........", "SERVER RECOVERY PRE1.............................");
                            Log.i("..", "...............................................");

                            String port1 = string_msg.substring(a + 2, string_msg.length());

                            String sel = "pre1";

                            Cursor c;
                            //c = db.queryDb(sel, port1);
                            String s2[]={"key","value","msg_port"};
                            c=db.getReadableDatabase().query("original",s2,"(msg_port="+port1+")",null,null,null,null,null);
                            c.moveToFirst();
                            Log.i("key",(c.getCount()+""));

                            String msg;
                            if (c.getCount() == 0) {
                                msg = "";

                            } else {
                                String key = c.getString(0);
                                String value = c.getString(1);
                                String port = c.getString(2);
                                msg = key + "," + value + "," + port;
                                c.moveToNext();
                                for (int i = 0; i < c.getCount() - 1; i++) {

                                    key = c.getString(0);
                                    value = c.getString(1);
                                    port = c.getString(2);
                                    msg = msg + "," + key + "," + value + "," + port;
                                    c.moveToNext();
                                }
                            }


                            Log.i("server-recovery-pre1", msg);
                            oss = s1.getOutputStream();
                            pws = new PrintWriter(oss);
                            pws.write(msg + "\n");
                            pws.flush();
                            Log.i("SERVER", "reached5");
                            pws.close();
                            brs.close();
                            s1.close();


                        }







                        else if (reqType.equals("awake")) {

                            TEST=true;
                            Log.e("SERVER",TEST+"");



                            oss = s1.getOutputStream();
                            pws = new PrintWriter(oss);
                            pws.write(acks + "\n");
                            pws.flush();
                            Log.i("SERVER", "reached5");
                            pws.close();
                            brs.close();
                            s1.close();


                        }


                        else if (reqType.equals("sleep")) {

                            TEST=false;
                            Log.e("SERVER",TEST+"");



                            oss = s1.getOutputStream();
                            pws = new PrintWriter(oss);
                            pws.write(acks + "\n");
                            pws.flush();
                            Log.i("SERVER", "reached5");
                            pws.close();
                            brs.close();
                            s1.close();


                        }







                        else if (reqType.equals("hash")) {

                            Log.i("SERVER", "reached hash");
                            String msgText;
                            String msgKey;
                            String p;
                            String temp = string_msg.substring(a + 2, string_msg.length());
                            a = temp.indexOf(",,");
                            msgKey = temp.substring(0, a);
                            String temp1 = temp.substring(a + 2, temp.length());
                            a = temp1.indexOf(",,");
                            msgText = temp1.substring(0, a);
                            p = temp1.substring(a + 2, temp1.length());


                            Log.i("SERVER - key", msgKey);
                            Log.i("SERVER - text", msgText);

                        /*Uri.Builder ub;
                        Uri uri;
                        ub=new Uri.Builder();
                        ub.authority("edu.buffalo.cse.cse486586.simpledynamo.provider");
                        uri=ub.build();
                        */
                            /*ContentValues cv = new ContentValues();
                            cv.put("key", msgKey);
                            cv.put("value", msgText);
                            //cv.put("msg_type", reqType);
                            cv.put("msg_port", p);
                            //insert(uri,cv);
                            db.insertIntoDb(cv, "true");*/

                            data.put(msgKey,msgText);

                            Log.e("size ",data.toString());
                            oss = s1.getOutputStream();
                            pws = new PrintWriter(oss);
                            pws.write(acks + "\n");
                            pws.flush();
                            Log.i("SERVER", "reached5");
                            //publishProgress(string_msg);
                            pws.close();
                            brs.close();
                            s1.close();

                        }










                        else if (reqType.equals("test")) {

                            Log.i("..", "...............................................");
                            Log.i("...........", "SERVER RECOVERY TEST.............................");
                            Log.i("..", "...............................................");

                            /*String temp = string_msg.substring(a + 2, string_msg.length());
                            int b = temp.indexOf(",,");
                            String port1 = temp.substring(0, b);
                            String port2 = temp.substring(b + 2, temp.length());

                            String sel = "rep";

                            Cursor c;
                            //c = db.queryDb(sel, port1+",,"+port2);
                            String s2[]={"key","value","msg_port"};
                            c=db.getReadableDatabase().query("original",s2,"(msg_port="+port1+")OR(msg_port="+port2+")",null,null,null,null,null);
                            c.moveToFirst();
                            Log.i("key",(c.getCount()+""));
*/
                            /*String msg;
                            if (c.getCount() == 0) {
                                msg = "";

                            } else {
                                String key = c.getString(0);
                                String value = c.getString(1);
                                String port = c.getString(2);
                                msg = key + "," + value + "," + port;
                                c.moveToNext();
                                for (int i = 0; i < c.getCount() - 1; i++) {

                                    key = c.getString(0);
                                    value = c.getString(1);
                                    port = c.getString(2);
                                    msg = msg + "," + key + "," + value + "," + port;
                                    c.moveToNext();
                                }
                            }*/

                            String msg="";
                            if(data.size()>0)
                            {
                                Set<String> set=data.keySet();
                                Iterator<String> it=set.iterator();
                                Log.e("data",data.toString());
                                Log.e("data",data.size()+"");
                                String y=it.next();
                                String key = y;
                                String value = data.get(y);
                                String port = "1111";
                                msg = key + "," + value + "," + port;
                                while(it.hasNext())
                                {
                                    String x=it.next();
                                    key = x;
                                    value = data.get(x);
                                    port = "1111";
                                    msg = msg+","+key + "," + value + "," + port;
                                }



                            }

                            data.clear();

                            Log.i("server-recovery-rep", msg);
                            oss = s1.getOutputStream();
                            pws = new PrintWriter(oss);
                            pws.write(msg + "\n");
                            pws.flush();
                            Log.i("SERVER", "reached5");
                            pws.close();
                            brs.close();
                            s1.close();


                        }























                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;


        }
    }









/*

    private class ClientTask extends AsyncTask<String, Void, Void>
    {
        OutputStream osc;
        PrintWriter pwc;
        InputStream isc;
        InputStreamReader isrc;
        BufferedReader brc;
        Socket socket;
        String remotePort;
        String reqType;
        String msg;
        String ackc;
        ContentValues cv1 = new ContentValues();
        protected Void doInBackground(String... msgs)
        {


            try {












                */
/*for (int i = 0; i < ports.size(); i++)
                {
                    if (myPort.equals(ports.get(i)))
                    {
                        TEST=true;
                    }
                    else
                    {
                        try
                        {
                            OutputStream osc;
                            PrintWriter pwc;
                            InputStream isc;
                            InputStreamReader isrc;
                            BufferedReader brc;
                            String remotePort = (Integer.parseInt(ports.get(i)) * 2) + "";
                            String req = "sleep"+",,";
                            String msg = req + "\n";

                            Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                    (Integer.parseInt(remotePort)));
                            osc = socket.getOutputStream();
                            pwc = new PrintWriter(osc);
                            pwc.write(msg);
                            pwc.flush();
                            isc = socket.getInputStream();
                            isrc = new InputStreamReader(isc);
                            brc = new BufferedReader(isrc);
                            String x = brc.readLine();
                            if (!(x.equals("")))
                            {
                                pwc.close();
                                brc.close();
                                socket.close();
                            }
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                }*//*







































                msg=msgs[0];
                String ports[]=new String[2];
                String pre1,pre2,rep;

                int a = msg.indexOf(",,");
                rep = msg.substring(0, a);
                String temp = msg.substring(a + 2, msg.length());
                a = temp.indexOf(",,");
                pre1 = temp.substring(0, a);
                pre2 = temp.substring(a + 2, temp.length());

                ports[0]=rep;
                ports[1]=pre1;
                //
                //reqType=msgs[1];

                String res="";
                for(int i=0;i<ports.length;i++)
                {
                    Log.e("recovery starts","************************************************************");
                Log.i("-----------------------","-----------------------------");
                Log.i("---","--------------CLIENT RECOVERY--------------------------");
                Log.i("-----------------------","-----------------------------");

                remotePort = ports[i];
                String msgToSend;
                if(remotePort.equals(rep))
                {
                    msgToSend=pre1+",,"+myPort;
                    remotePort=rep;
                    reqType="rep";
                }
                else
                {
                    //remotePort=rep;
                    msgToSend=pre2;
                    remotePort=pre1;
                    reqType="pre1";
                }

                String send=reqType+",,"+msgToSend+"\n";


                remotePort=(Integer.parseInt(remotePort)*2)+"";

                Log.i("CLIENT-"+reqType.toUpperCase(), remotePort);
                Log.i("CLIENT-"+reqType.toUpperCase(), send);


                socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        (Integer.parseInt(remotePort)));

                osc = socket.getOutputStream();
                pwc = new PrintWriter(osc);
                pwc.write(send);
                pwc.flush();

                Log.i("CLIENT", "client side reached1");

                isc = socket.getInputStream();
                isrc = new InputStreamReader(isc);
                Log.i("CLIENT", "client side reached2");
                brc = new BufferedReader(isrc);
                Log.i("CLIENT", "client side reached4");

                String x=brc.readLine();
                if (!(x.equals("")))
                {
                    Log.i("CLIENT", "client side reached5");
                    pwc.close();
                    brc.close();
                    socket.close();
                    res=x;

                    Log.i("----result-----",res);



                    StringTokenizer st = new StringTokenizer(res, ",");
                    while (st.hasMoreTokens()) {

                        //Log.i("testing",st.nextToken());

                        String k = st.nextToken();
                        String v = st.nextToken();
                        String p = st.nextToken();
                        db.getWritableDatabase().execSQL("insert or replace into original(key,value,msg_port) values('"+k+"','"+v+"','"+p+"')");


                        //String p = st.nextToken();
                        //Log.i("key",k);
                        //Log.i("value",v);
                        //Log.i("type",t);
                        //Log.i("port",p);

                        */
/*try
                        {
                            //db.getWritableDatabase().beginTransaction();
                            *//*
*/
/*ContentValues cv1=new ContentValues();
                            cv1.put("key", k);
                            cv1.put("value", v);
                            cv1.put("msg_port",p);
                            *//*
*/
/*//*
/Log.i("CV contents",cv1.toString());
                            *//*
*/
/*long l=db.getWritableDatabase().insertWithOnConflict("original", null, cv1, SQLiteDatabase.CONFLICT_REPLACE);
                            if (l == -1)
                                Log.i("DbProvider", "error occured");
                            else
                                Log.i("db provider","insert in DB done");
                            *//*
*/
/*//*
/db.getWritableDatabase().insertWithOnConflict("original", null, cv1, SQLiteDatabase.CONFLICT_REPLACE);


                        }
                        finally {
                               db.getWritableDatabase().endTransaction();
                        }
*//*


                        //db.getWritableDatabase().execSQL("insert or replace into original(key,value,msg_port) values('"+k+"','"+v+"','"+p+"')");

                        //cv1.put("key", k);
                        //cv1.put("value", v);
                        // cv1.put("msg_type",t);
                        //cv1.put("msg_port",p);
                        //Log.i("CV contents",cv1.toString());
                        //db.insertIntoDb(cv1, "true");
                        //db.getWritableDatabase().insertWithOnConflict("original", null, cv1, SQLiteDatabase.CONFLICT_REPLACE);

                    }


                    //Log.i("CLIENT - SIZE OF CV", cv1.size()+"=================================");


                }
                else
                {
                    Log.i("CLIENT", "got NULL from server");
                }


                // Log.i("result",res);

                }
                //db.insertIntoDb(cv1, "true");
                //db.getWritableDatabase().insertWithOnConflict("original", null, cv1, SQLiteDatabase.CONFLICT_REPLACE);



            } catch (Exception e) {
                e.printStackTrace();


            }















           */
/* for (int i = 0; i < ports.size(); i++)
            {
                if (myPort.equals(ports.get(i)))
                {
                    TEST=true;
                }
                else
                {
                    try
                    {
                        OutputStream osc;
                        PrintWriter pwc;
                        InputStream isc;
                        InputStreamReader isrc;
                        BufferedReader brc;
                        String remotePort = (Integer.parseInt(ports.get(i)) * 2) + "";
                        String req = "awake"+",,";
                        String msg = req + "\n";

                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                (Integer.parseInt(remotePort)));
                        osc = socket.getOutputStream();
                        pwc = new PrintWriter(osc);
                        pwc.write(msg);
                        pwc.flush();
                        isc = socket.getInputStream();
                        isrc = new InputStreamReader(isc);
                        brc = new BufferedReader(isrc);
                        String x = brc.readLine();
                        if (!(x.equals("")))
                        {
                            pwc.close();
                            brc.close();
                            socket.close();
                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }
*//*






        Log.i(" client reached here", "*******************************************");
            Log.e("recovery ends","*******************************************");
            return null;
        }
    }






*/













    private class ClientTask extends AsyncTask<String, Void, Void>
    {
        OutputStream osc;
        PrintWriter pwc;
        InputStream isc;
        InputStreamReader isrc;
        BufferedReader brc;
        Socket socket;
        String remotePort;
        String reqType;
        String msg;
        String ackc;

        protected Void doInBackground(String... msgs)
        {


            try {







                for (int i = 0; i < ports.size(); i++)
                {
                    if (myPort.equals(ports.get(i)))
                    {
                        TEST=true;
                    }
                    else
                    {
                        try
                        {
                            OutputStream osc;
                            PrintWriter pwc;
                            InputStream isc;
                            InputStreamReader isrc;
                            BufferedReader brc;
                            String remotePort = (Integer.parseInt(ports.get(i)) * 2) + "";
                            String req = "sleep"+",,";
                            String msg = req + "\n";

                            Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                    (Integer.parseInt(remotePort)));
                            osc = socket.getOutputStream();
                            pwc = new PrintWriter(osc);
                            pwc.write(msg);
                            pwc.flush();
                            isc = socket.getInputStream();
                            isrc = new InputStreamReader(isc);
                            brc = new BufferedReader(isrc);
                            String x2 = brc.readLine();
                            if (!(x2.equals("")))
                            {
                                pwc.close();
                                brc.close();
                                socket.close();
                            }
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                }

















                msg=msgs[0];
                //String ports[]=new String[2];
                String pre1,pre2,rep;

                int a = msg.indexOf(",,");
                rep = msg.substring(0, a);
                String temp = msg.substring(a + 2, msg.length());
                a = temp.indexOf(",,");
                pre1 = temp.substring(0, a);
                pre2 = temp.substring(a + 2, temp.length());

                //ports[0]=rep;
                //ports[1]=pre1;
                //
                reqType=msgs[1];

                String res="";
                //for(int i=0;i<ports.length;i++)
                //{


                Log.i("-----------------------","-----------------------------");
                Log.i("---","--------------CLIENT RECOVERY--------------------------");
                Log.i("-----------------------","-----------------------------");

                //remotePort = ports[i];
                String msgToSend;
                if(reqType.equals("rep"))
                {
                    msgToSend=pre1+",,"+myPort;
                    remotePort=rep;
                    //reqType="rep";
                }
                else
                {
                    remotePort=rep;
                    msgToSend=pre2;
                    //remotePort=pre1;
                    //reqType="pre1";
                }

                String send=reqType+",,"+msgToSend+"\n";


                remotePort=(Integer.parseInt(remotePort)*2)+"";

                Log.i("CLIENT-"+reqType.toUpperCase(), remotePort);
                Log.i("CLIENT-"+reqType.toUpperCase(), send);


                socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        (Integer.parseInt(remotePort)));

                osc = socket.getOutputStream();
                pwc = new PrintWriter(osc);
                pwc.write(send);
                pwc.flush();

                Log.i("CLIENT", "client side reached1");

                isc = socket.getInputStream();
                isrc = new InputStreamReader(isc);
                Log.i("CLIENT", "client side reached2");
                brc = new BufferedReader(isrc);
                Log.i("CLIENT", "client side reached4");

                String x=brc.readLine();
                if (!(x.equals("")))
                {
                    Log.i("CLIENT", "client side reached5");
                    pwc.close();
                    brc.close();
                    socket.close();
                    res=x;

                    Log.i("----result-----",res);


                    StringTokenizer st = new StringTokenizer(res, ",");
                    while (st.hasMoreTokens()) {

                        //Log.i("testing",st.nextToken());

                        String k = st.nextToken();
                        String v = st.nextToken();
                        String p = st.nextToken();
                        //String p = st.nextToken();
                        Log.i("key",k);
                        Log.i("value",v);
                        //Log.i("type",t);
                        Log.i("port",p);

                        ContentValues cv1 = new ContentValues();
                        cv1.put("key", k);
                        cv1.put("value", v);

                        Cursor c=db.queryDb(k,"true");
                        if(c.getCount()==0)
                        {
                            cv1.put("msg_port","1");
                        }
                        else
                        {
                            int t=Integer.parseInt(c.getString(2));
                            t++;
                            cv1.put("msg_port",t+"");
                        }


                        // cv1.put("msg_type",t);
                        //cv1.put("msg_port",p);
                        //db.insertIntoDb(cv1, "true");
                        db.getWritableDatabase().insertWithOnConflict("original", null, cv1, SQLiteDatabase.CONFLICT_REPLACE);

                    }





                }
                else
                {
                    Log.i("CLIENT", "got NULL from server");
                }


                // Log.i("result",res);

                //}


















                for (int i = 0; i < ports.size(); i++)
                {
                    if (myPort.equals(ports.get(i)))
                    {
                        TEST=true;
                    }
                    else
                    {
                        try
                        {
                            OutputStream osc;
                            PrintWriter pwc;
                            InputStream isc;
                            InputStreamReader isrc;
                            BufferedReader brc;
                            String remotePort = (Integer.parseInt(ports.get(i)) * 2) + "";
                            String req = "awake"+",,";
                            String msg = req + "\n";

                            Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                    (Integer.parseInt(remotePort)));
                            osc = socket.getOutputStream();
                            pwc = new PrintWriter(osc);
                            pwc.write(msg);
                            pwc.flush();
                            isc = socket.getInputStream();
                            isrc = new InputStreamReader(isc);
                            brc = new BufferedReader(isrc);
                            String x1 = brc.readLine();
                            if (!(x1.equals("")))
                            {
                                pwc.close();
                                brc.close();
                                socket.close();
                            }
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                }






















            } catch (Exception e) {
                e.printStackTrace();


            }


            Log.i(" client reached here", "*******************************************");

            return null;
        }
    }
















}