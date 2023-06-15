package org.example;
import java.math.BigDecimal;
import java.util.concurrent.locks.Lock;

import com.binance.connector.client.enums.DefaultUrls;
import com.binance.connector.client.impl.WebsocketApiClientImpl;
import com.binance.connector.client.impl.SpotClientImpl;
import com.binance.connector.client.impl.spot.Market;

import java.util.*;

import org.json.JSONObject;
import com.binance.connector.client.utils.HmacSignatureGenerator;
import org.example.PrivateConfig;

import com.binance.connector.client.impl.WebsocketStreamClientImpl;
import com.binance.connector.client.exceptions.BinanceClientException;
import com.binance.connector.client.exceptions.BinanceConnectorException;

import java.util.LinkedHashMap;


import static java.math.BigDecimal.ROUND_DOWN;
import static org.jocl.CL.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.locks.ReentrantLock;

import org.jocl.*;

import org.json.*;


class Main{
    static final int VAY_LEN = 16;
    static SpotClientImpl spotClient;
    static SpotClientImpl[] spot_clirnts;
    static HmacSignatureGenerator signatureGenerator;
    static WebsocketApiClientImpl wstradeclient;
    static HashMap<String,int[]> alltickers;
    static HashMap<String,Long> alltickers_in_vays = new HashMap<>();
    static List<ArrayList<Integer>> can_translate;
    static ArrayList <String> allvalues;
    static ArrayList <Float> mybalance;
    static double[][][] tickers;
    static float[][][] limits;


    static ArrayList<ArrayList<Integer>> vays = new ArrayList<ArrayList<Integer>>();

    static double round(double num,int i){
        BigDecimal c = new BigDecimal(num+"");
        c = c.setScale(i,ROUND_DOWN);
        return c.doubleValue();
    }
//    private static void rec(ArrayList<Integer> v,int flag){
//        ArrayList<Integer> vay = new ArrayList<>(v);
//        vay.add(-1);
//        int b;
////        System.out.println((vay.size()-2));
//        for (int i = 0; i < can_translate.get((vay.get(vay.size()-2))).size(); i++) {
//            b = can_translate.get((vay.get(vay.size()-2))).get(i);
//            if(vay.contains(b)){
//                if(vay.indexOf(b)<=flag){
//                    v = new ArrayList<>();
//                    for (int j = vay.indexOf(b); j < vay.size(); j++) {
//                        v.add(vay.get(j));
//                    }
//                    v.set(v.size()-1,b);
//
//                    if(v.size()>3) {
//                        vays.add(v);
//                    }
//                }
//            }
//            else{
//                if(vay.size()<VAY_LEN-1) {
//                    vay.set(vay.size() - 1, b);
//                    rec(vay, flag);
//                    flag = Math.min(flag, vay.size() - 1);
//                }
//            }
//
//        }
//    }
    private static void rec(ArrayList<Integer> v){
        ArrayList<Integer> vay = new ArrayList<>(v);
        vay.add(-1);
        int b;
        for (int i = 0; i < can_translate.get((vay.get(vay.size()-2))).size(); i++) {
            b = can_translate.get((vay.get(vay.size()-2))).get(i);
            if(vay.get(0)==b){
                vay.set(vay.size() - 1, b);
                if(vay.size()>3) {
                    vays.add(vay);
                }
            }
            else if(vay.size()<VAY_LEN && !vay.contains(b)) {
                    vay.set(vay.size() - 1, b);
                    rec(new ArrayList<>(vay));
            }
        }
    }

    static final Object sync = new Object();
    public static cl_command_queue commandQueue;

    static cl_kernel kernel_add;
    static cl_kernel kernel_calc;

    static cl_mem clvays;
    static cl_mem clans;
    static cl_mem sizes;
    static cl_mem cltickers;
    static long time = 0;

    static long[] global_work_size;
    static long[] time_of_plus;
    private static void buy_http(LinkedHashMap params,int[] i){
//        System.out.println(System.currentTimeMillis()+" trade "+ Arrays.toString(i));
//        System.out.println(params);
        String result = spot_clirnts[i[0]].createTrade().newOrder(params);
        System.out.println(result);
    }
    private static void lens_buy(int vay_num){
        System.out.println("I'm buying");
                LinkedHashMap params;

                for (int i = 0; i < vays.get(vay_num).size(); i++) {
                    System.out.print(allvalues.get(vays.get(vay_num).get(i)) + " " + tickers[vays.get(vay_num).get(i)][vays.get(vay_num).get((i + 1) % (vays.get(vay_num).size()))][0] + " ");
                }
                double sum = 16;

                for (int index = 0; index < vays.get(vay_num).size()-1; index++) {

                    if (!Objects.equals(vays.get(vay_num).get(index), vays.get(vay_num).get(index + 1))) {
                        params = new LinkedHashMap<>();
//                        params.put("returnRateLimits",false);
                        if ((limits[vays.get(vay_num).get(index)][vays.get(vay_num).get(index + 1)][3] != 0)) {
                            System.out.println((allvalues.get(vays.get(vay_num).get(index)) + "" + allvalues.get(vays.get(vay_num).get(index + 1)).toUpperCase())+" "+params.get("quantity")+" "+System.currentTimeMillis());
//                            params.put("quantity", round(sum, (int) limits[vays.get(vay_num).get(index)][vays.get(vay_num).get(index + 1)][2]));
//                            buy((allvalues.get(vays.get(vay_num).get(index)) + "" + allvalues.get(vays.get(vay_num).get(index + 1)).toUpperCase()),"SELL","MARKET",params);

                            params.put("symbol", (allvalues.get(vays.get(vay_num).get(index)) + "" + allvalues.get(vays.get(vay_num).get(index + 1)).toUpperCase()));
                            params.put("side", "SELL");
                            params.put("type", "MARKET");
//                            params.put("timeInForce", "GTC");
                            params.put("quantity", round(sum, (int) limits[vays.get(vay_num).get(index)][vays.get(vay_num).get(index + 1)][2]));

                            int[] m = new int[]{index};
                            System.out.println("last_cher "+params);
                            LinkedHashMap finalParams = params;
                            new Thread(() -> buy_http(finalParams, m)).start();

                            sum *= tickers[vays.get(vay_num).get(index)][vays.get(vay_num).get(index + 1)][0];

                        }
                        else  {
                            sum *= tickers[vays.get(vay_num).get(index)][vays.get(vay_num).get(index +1)][0];
                            System.out.println((allvalues.get(vays.get(vay_num).get(index + 1)) + "" + allvalues.get(vays.get(vay_num).get(index)).toUpperCase())+" "+params.get("quantity")+" "+System.currentTimeMillis());
//                            params.put("quantity", round(sum, (int) limits[vays.get(vay_num).get(index + 1)][vays.get(vay_num).get(index)][2]));
//                            buy((allvalues.get(vays.get(vay_num).get(index + 1)) + "" + allvalues.get(vays.get(vay_num).get(index))).toUpperCase(),"BUY","MARKET",params);

                            params.put("symbol", (allvalues.get(vays.get(vay_num).get(index + 1)) + "" + allvalues.get(vays.get(vay_num).get(index))).toUpperCase());
                            params.put("side", "BUY");
                            params.put("type", "MARKET");
//                            params.put("timeInForce", "GTC");
                            params.put("quantity", round(sum, (int) limits[vays.get(vay_num).get(index + 1)][vays.get(vay_num).get(index)][2]));

                            int[] m = new int[]{index};
//                            System.out.println("last_cher "+params);
                            LinkedHashMap finalParams = params;
                            new Thread(() -> buy_http(finalParams, m)).start();
                        }
                        System.out.println(sum);
                    }
                }
//                System.out.println("stop convert "+System.currentTimeMillis());
    }
    static boolean was_trade = false;
    private static void run_calc() throws InterruptedException {
        JSONObject params;
        double[] ans = new double[vays.size()];
//        System.out.println("start_calc"+" "+System.currentTimeMillis());
        synchronized (sync) {
            clSetKernelArg(kernel_calc, 0, Sizeof.cl_mem, Pointer.to(sizes));
            clSetKernelArg(kernel_calc, 1, Sizeof.cl_mem, Pointer.to(cltickers));
            clSetKernelArg(kernel_calc, 2, Sizeof.cl_mem, Pointer.to(clvays));
            clSetKernelArg(kernel_calc, 3, Sizeof.cl_mem, Pointer.to(clans));

            clEnqueueNDRangeKernel(commandQueue, kernel_calc, 1, null,
                    global_work_size, null, 0, null, null);
            clEnqueueReadBuffer(commandQueue, clans, CL_TRUE, 0,
                    (long) ans.length * Sizeof.cl_double, Pointer.to(ans), 0, null, null);
            }
            double max = 0;
            int vay_num = -1;
            int index;
//            ArrayList<Integer> plus_vays = new ArrayList<>();

            for (int i = 0; i < ans.length; i++) {
                if (ans[i]>1){
                    if (time_of_plus[i]==0){
                        time_of_plus[i] = System.currentTimeMillis();
                    }
//                    else if(System.currentTimeMillis()-time_of_plus[i]>600){
//                        vay_num = i;
//                    }
                }
                else{
                    if (time_of_plus[i]!=0){
                        for (int j = 0; j < vays.get(i).size(); j++) {
                            System.out.print(allvalues.get(vays.get(i).get(j)) + " " + tickers[vays.get(i).get(j)][vays.get(i).get((j + 1) % (vays.get(i).size()))][0] +"   ");
                        }
                        System.out.println("   " + (System.currentTimeMillis()-time_of_plus[i]));
                        time_of_plus[i]=0;
                    }
                }
//                if (ans[i]>max){
//                    max = ans[i];
//                    vay_num = i;

//                    for (int j = 0; j < vays.get(i).size(); j++) {
//                        System.out.print(allvalues.get(vays.get(i).get(j)) + " " + tickers[vays.get(i).get(j)][vays.get(i).get((j + 1) % (vays.get(i).size()))][0] + " ");
//                    }
//                    System.out.println(max);
//                }
            }
//        System.out.println(max);


        double sum;

//            if (vay_num>-1) {
            if (false){
                    for (int i = 0; i < vays.get(vay_num).size(); i++) {
                        System.out.print(allvalues.get(vays.get(vay_num).get(i)) + " " + tickers[vays.get(vay_num).get(i)][vays.get(vay_num).get((i + 1) % (vays.get(vay_num).size()))][0] + " ");
                    }
                    sum = 16;

                    for (index = 0; index < vays.get(vay_num).size()-1; index++) {

                        if (!Objects.equals(vays.get(vay_num).get(index), vays.get(vay_num).get(index + 1))) {
//                        params = new LinkedHashMap<>();
                            params = new JSONObject();
//                        params.put("returnRateLimits",false);
                            if ((limits[vays.get(vay_num).get(index)][vays.get(vay_num).get(index + 1)][3] != 0)) {
                                params.put("quantity", round(sum, (int) limits[vays.get(vay_num).get(index)][vays.get(vay_num).get(index + 1)][2]));
                                System.out.println((allvalues.get(vays.get(vay_num).get(index)) + "" + allvalues.get(vays.get(vay_num).get(index + 1)).toUpperCase())+" "+params.get("quantity")+" "+System.currentTimeMillis());
                                wstradeclient.trade().newOrder((allvalues.get(vays.get(vay_num).get(index)) + "" + allvalues.get(vays.get(vay_num).get(index + 1)).toUpperCase()),"SELL","MARKET",params);

//                                wstradeclient.trade().newOrder();
//                            params.put("symbol", (allvalues.get(vays.get(vay_num).get(index)) + "" + allvalues.get(vays.get(vay_num).get(index + 1)).toUpperCase()));
//                            params.put("side", "SELL");
//                            params.put("type", "MARKET");
////                            params.put("timeInForce", "GTC");
//                            params.put("quantity", round(sum, (int) limits[vays.get(vay_num).get(index)][vays.get(vay_num).get(index + 1)][2]));
//
//                            int[] m = new int[]{index};
//                            System.out.println("last_cher "+params);
//                            LinkedHashMap finalParams = params;
//                            new Thread(() -> buy_http(finalParams, m)).start();

                                sum *= tickers[vays.get(vay_num).get(index)][vays.get(vay_num).get(index + 1)][0];

                            }
                            else  {
                                sum *= tickers[vays.get(vay_num).get(index)][vays.get(vay_num).get(index +1)][0];
                                params.put("quantity", round(sum, (int) limits[vays.get(vay_num).get(index + 1)][vays.get(vay_num).get(index)][2]));
                                System.out.println((allvalues.get(vays.get(vay_num).get(index + 1)) + "" + allvalues.get(vays.get(vay_num).get(index)).toUpperCase())+" "+params.get("quantity")+" "+System.currentTimeMillis());
                                wstradeclient.trade().newOrder((allvalues.get(vays.get(vay_num).get(index + 1)) + "" + allvalues.get(vays.get(vay_num).get(index))).toUpperCase(),"BUY","MARKET",params);

//                            params.put("symbol", (allvalues.get(vays.get(vay_num).get(index + 1)) + "" + allvalues.get(vays.get(vay_num).get(index))).toUpperCase());
//                            params.put("side", "BUY");
//                            params.put("type", "MARKET");
////                            params.put("timeInForce", "GTC");
//                            params.put("quantity", round(sum, (int) limits[vays.get(vay_num).get(index + 1)][vays.get(vay_num).get(index)][2]));
//
//                            int[] m = new int[]{index};
////                            System.out.println("last_cher "+params);
//                            LinkedHashMap finalParams = params;
//                            new Thread(() -> buy_http(finalParams, m)).start();
                            }
                            System.out.println(sum);
                        }
                    }
                    vay_num = -1;
//                System.out.println("stop convert "+System.currentTimeMillis());
//                }
            }
//            else{
//        Thread.sleep(300);
                synchronized (lock) {
                    lock = false;
                }

//            }
    }

    static String[] parse(String s){
        String[] ans = new String[5];
        ans[0] = s.substring(s.indexOf("\"a\""),s.indexOf("\"A\""));
        ans[1] = s.substring(s.indexOf("\"A\""),s.indexOf("}}"));
        ans[2] = s.substring(s.indexOf("\"b\""),s.indexOf("\"B\""));
        ans[3] = s.substring(s.indexOf("\"B\""),s.indexOf("\"a\""));
        ans[4] = s.substring(s.indexOf("\"s\""),s.indexOf("\"b\""));
        return ans;
    }

    static Boolean lock = false;
    static Integer vay_num = 0;
    static ArrayList<String> first_stream;
    public static void main(String[] args) throws IOException, InterruptedException {

        signatureGenerator = new HmacSignatureGenerator(PrivateConfig.SECRET_KEY);
        wstradeclient = new WebsocketApiClientImpl(PrivateConfig.API_KEY, signatureGenerator, DefaultUrls.WS_API_URL);
        wstradeclient.connect(((message) -> {
//            System.out.println(System.currentTimeMillis());
            System.out.println(message);
        }));
//        System.out.println("ready");
//        Thread.sleep(6000);
//        JSONObject param = new JSONObject();
//        param.put("requestId", "randomId");
//        param.put("returnRateLimits",false);
//        param.put("quantity", 15);
//
//        System.out.println(System.currentTimeMillis());
//        wstradeclient.trade().newOrder("COCOSUSDT", "BUY", "MARKET", param);
//        System.out.println(System.currentTimeMillis());
//        Thread.sleep(600000);
        lock = true;

        Path p = Paths.get("/home/kirill/IdeaProjects/f_t/src/main/java/org/example/kernel");
        String programSource = Files.readString(p, StandardCharsets.UTF_8);
        final int platformIndex = 1;
        final long deviceType = CL_DEVICE_TYPE_ALL;
        final int deviceIndex = 0;
        CL.setExceptionsEnabled(true);
        int numPlatformsArray[] = new int[1];
        clGetPlatformIDs(0, null, numPlatformsArray);
        int numPlatforms = numPlatformsArray[0];
        cl_platform_id platforms[] = new cl_platform_id[numPlatforms];
        clGetPlatformIDs(platforms.length, platforms, null);
        cl_platform_id platform = platforms[platformIndex];
        cl_context_properties contextProperties = new cl_context_properties();
        contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform);
        int numDevicesArray[] = new int[1];
        clGetDeviceIDs(platform, deviceType, 0, null, numDevicesArray);
        int numDevices = numDevicesArray[0];
        cl_device_id devices[] = new cl_device_id[numDevices];
        clGetDeviceIDs(platform, deviceType, numDevices, devices, null);
        cl_device_id device = devices[deviceIndex];
        cl_context context = clCreateContext(
                contextProperties, 1, new cl_device_id[]{device},
                null, null, null);
        cl_queue_properties properties = new cl_queue_properties();
        commandQueue = clCreateCommandQueueWithProperties(
                context, device, properties, null);

        cl_program program_calc = clCreateProgramWithSource(context,
                1, new String[]{ programSource }, null, null);
        clBuildProgram(program_calc, 0, null, null, null, null);
        kernel_calc = clCreateKernel(program_calc, "calc", null);

        cl_program program_add = clCreateProgramWithSource(context,
                1, new String[]{ programSource }, null, null);
        clBuildProgram(program_add, 0, null, null, null, null);
        kernel_add = clCreateKernel(program_add, "add", null);


        allvalues = new ArrayList<>();
        mybalance = new ArrayList<>();

        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
        spotClient = new SpotClientImpl(PrivateConfig.API_KEY, PrivateConfig.SECRET_KEY);

        spot_clirnts = new SpotClientImpl[VAY_LEN];
        for (int i = 0; i < VAY_LEN; i++) {
            spot_clirnts[i] = new SpotClientImpl(PrivateConfig.API_KEY, PrivateConfig.SECRET_KEY);
        }

        String result = "";
        try {
            result = spotClient.createTrade().account(parameters);
        } catch (BinanceConnectorException | BinanceClientException exception) {
            System.out.println(exception);
        }
        JSONObject a = new JSONObject(result);
        JSONArray b = a.getJSONArray("balances");
        System.out.println(a.toString());
        for (int i = 0; i < b.length(); i++) {
            a = b.getJSONObject(i);
            if(a.getFloat("free")>0){
//            if(true){
                mybalance.add(a.getFloat("free"));
                allvalues.add(a.getString("asset"));
            }
        }
        System.out.println(mybalance.toString());
        System.out.println(allvalues.toString());
        System.out.println(allvalues.size());

        WebsocketStreamClientImpl wsClient = new WebsocketStreamClientImpl();


        tickers = new double[allvalues.size()][allvalues.size()][2];
        limits = new float[allvalues.size()][allvalues.size()][6];
        can_translate = new ArrayList<>();
        for (int i = 0; i <allvalues.size(); i++) {
            ArrayList<Integer> v = new ArrayList<>();
            can_translate.add(v);
        }


        double[] tickers1d = new double[tickers.length*tickers.length];
        for (int i = 0; i < tickers.length; i++) {
            for (int j = 0; j < tickers[i].length; j++) {
                tickers1d[tickers.length*i+j] = 0;
            }
        }
        cltickers = clCreateBuffer(context,
                CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                (long) Sizeof.cl_double * tickers1d.length, Pointer.to(tickers1d), null);

        SpotClientImpl client = new SpotClientImpl();
        Market market = client.createMarket();
        parameters = new LinkedHashMap<>();

        result = market.exchangeInfo(parameters);
        JSONObject c = new JSONObject(result);
        b = c.getJSONArray("symbols");

        alltickers = new HashMap();

        int[] cl_i = new int[1];
        double[] cl_num = new double[1];
        ArrayList<String> stream_list = new ArrayList<>();
        int connections = 0;
        int connections_num = 0;
        first_stream = new ArrayList<>();

        int num_ticker = 0;
        //et limits [minQty,maxQty,stepSize,minNotional,baseAssetPrecision,quoteAssetPrecision] and streamsb.length()/10
        System.out.println(b.length()+" ojishdugfh");
        while (connections_num<64 && num_ticker<b.length()) {
            connections = 0;
            while (connections < 10 && num_ticker < b.length()) {
//        for (int i = 0; i < 10; i++) {
                a = b.getJSONObject(num_ticker);
                try {
                    if (allvalues.contains(a.getString("baseAsset")) && allvalues.contains(a.getString("quoteAsset")) && (a.getJSONArray("permissions").toList().contains("SPOT") || !a.keySet().contains("permissions")) && a.getString("status").equals("TRADING") && a.getJSONArray("orderTypes").toList().contains("MARKET")) {
                        connections++;
                        JSONObject c1 = a.getJSONArray("filters").getJSONObject(1);
                        JSONObject c2 = a.getJSONArray("filters").getJSONObject(4);

                        int[] l = new int[2];
                        l[0] = allvalues.indexOf(a.getString("baseAsset"));
                        l[1] = allvalues.indexOf(a.getString("quoteAsset"));

                        if (c1.getFloat("minQty") == 0 || c2.getFloat("minQty") == 0) {
                            limits[l[0]][l[1]][0] = Math.max(c1.getFloat("minQty"), c2.getFloat("minQty"));
                        } else {
                            limits[l[0]][l[1]][0] = Math.min(c1.getFloat("minQty"), c2.getFloat("minQty"));
                        }
                        if (c1.getFloat("maxQty") == 0 || c2.getFloat("maxQty") == 0) {
                            limits[l[0]][l[1]][1] = Math.max(c1.getFloat("maxQty"), c2.getFloat("maxQty"));
                        } else {
                            limits[l[0]][l[1]][1] = Math.min(c1.getFloat("maxQty"), c2.getFloat("maxQty"));
                        }
                        if (c1.getFloat("stepSize") == 0 || c2.getFloat("stepSize") == 0) {
                            limits[l[0]][l[1]][2] = Math.max(c1.getFloat("stepSize"), c2.getFloat("stepSize"));
                        } else {
                            limits[l[0]][l[1]][2] = Math.min(c1.getFloat("stepSize"), c2.getFloat("stepSize"));
                        }
                        limits[l[0]][l[1]][2] = String.format("%.8f",limits[l[0]][l[1]][2]).indexOf("1") - String.format("%.8f",limits[l[0]][l[1]][2]).indexOf(".");
                        if (limits[l[0]][l[1]][2]<0){
                            limits[l[0]][l[1]][2]++;
                        }
                        c = a.getJSONArray("filters").getJSONObject(2);
                        limits[l[0]][l[1]][3] = c.getFloat("minNotional");

                        limits[l[0]][l[1]][4] = a.getFloat("baseAssetPrecision");
                        limits[l[0]][l[1]][5] = a.getFloat("quoteAssetPrecision");
//                        if (limits[l[0]][l[1]][0]==0 || limits[l[0]][l[1]][1] == 0){
//                            System.out.println(limits[l[0]][l[1]][0]+" "+c1.getFloat("minQty")+" "+c2.getFloat("minQty")+" "+(c1.getFloat("minQty")==0)+" "+(c2.getFloat("minQty")==0)+" "+Math.max(c1.getFloat("minQty"), c2.getFloat("minQty"))+" "+Math.min(c1.getFloat("minQty"), c2.getFloat("minQty")));
//                            System.out.println(limits[l[0]][l[1]][1]+" "+c1.getFloat("maxQty")+" "+c2.getFloat("maxQty")+" "+(c1.getFloat("maxQty")==0)+" "+(c2.getFloat("maxQty")==0)+" "+Math.max(c1.getFloat("maxQty"), c2.getFloat("maxQty"))+" "+Math.min(c1.getFloat("maxQty"), c2.getFloat("maxQty")));
//                        }


                        alltickers.put(a.getString("baseAsset") + a.getString("quoteAsset"), l);

                        can_translate.get(l[0]).add(l[1]);
                        can_translate.get(l[1]).add(l[0]);

//                        Thread.sleep(250);

                        stream_list.add((allvalues.get(l[0])+allvalues.get(l[1])).toLowerCase()+"@bookTicker");

                        parameters = new LinkedHashMap<>();
                        parameters.put("symbol", a.getString("baseAsset") + a.getString("quoteAsset"));
                        parameters.put("limit", "1");
                        String res = client.createMarket().depth(parameters);
                        JSONObject aa = new JSONObject(res);

                        tickers[l[0]][l[1]][0] = aa.getJSONArray("bids").getJSONArray(0).getFloat(0);
                        tickers[l[0]][l[1]][1] = aa.getJSONArray("bids").getJSONArray(0).getFloat(1) / tickers[l[0]][l[1]][0];

                        tickers[l[1]][l[0]][0] = 1 / aa.getJSONArray("asks").getJSONArray(0).getFloat(0);
                        tickers[l[1]][l[0]][1] = aa.getJSONArray("asks").getJSONArray(0).getFloat(1);


                        synchronized (sync) {
                            cl_i[0] = l[0] * tickers[0].length + l[1];
                            cl_num[0] = tickers[l[0]][l[1]][0];
                            cl_mem cl_i_buffer = clCreateBuffer(context,
                                    CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                                    Sizeof.cl_int, Pointer.to(cl_i), null);
                            cl_mem cl_num_buffer = clCreateBuffer(context,
                                    CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                                    Sizeof.cl_double, Pointer.to(cl_num), null);

                            clSetKernelArg(kernel_add, 0, Sizeof.cl_mem, Pointer.to(cltickers));
                            clSetKernelArg(kernel_add, 1, Sizeof.cl_mem, Pointer.to(cl_i_buffer));
                            clSetKernelArg(kernel_add, 2, Sizeof.cl_mem, Pointer.to(cl_num_buffer));

                            clEnqueueNDRangeKernel(commandQueue, kernel_add, 1, null,
                                    new long[]{1}, null, 0, null, null);
                            clReleaseMemObject(cl_i_buffer);
                            clReleaseMemObject(cl_num_buffer);

                            cl_i[0] = l[1] * tickers[0].length + l[0];
                            cl_num[0] = tickers[l[1]][l[0]][0];
                            cl_i_buffer = clCreateBuffer(context,
                                    CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                                    Sizeof.cl_int, Pointer.to(cl_i), null);
                            cl_num_buffer = clCreateBuffer(context,
                                    CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                                    Sizeof.cl_double, Pointer.to(cl_num), null);

                            clSetKernelArg(kernel_add, 0, Sizeof.cl_mem, Pointer.to(cltickers));
                            clSetKernelArg(kernel_add, 1, Sizeof.cl_mem, Pointer.to(cl_i_buffer));
                            clSetKernelArg(kernel_add, 2, Sizeof.cl_mem, Pointer.to(cl_num_buffer));

                            clEnqueueNDRangeKernel(commandQueue, kernel_add, 1, null,
                                    new long[]{1}, null, 0, null, null);
                            clReleaseMemObject(cl_i_buffer);
                            clReleaseMemObject(cl_num_buffer);
                        }
                    }
                } catch (Exception ignored) {
                }
                num_ticker++;
            }
            System.out.println(stream_list.toString());

            wsClient.combineStreams(stream_list, ((event) -> {
//                System.out.println("get_m "+System.currentTimeMillis());
                JSONObject aa = new JSONObject(event);
//                System.out.println(aa);
                aa = aa.getJSONObject("data");
                int[] bb = alltickers.get(aa.getString("s"));

                tickers[bb[0]][bb[1]][0] = aa.getFloat("b");
                tickers[bb[0]][bb[1]][1] = aa.getFloat("B") / tickers[bb[0]][bb[1]][0];

                tickers[bb[1]][bb[0]][0] = 1 / aa.getFloat("a");
                tickers[bb[1]][bb[0]][1] = aa.getFloat("A");

                synchronized (sync) {
//                    System.out.println("appdate "+System.currentTimeMillis());

                    int[] cl_ii = new int[1];
                    double[] cl_numm = new double[1];

                    cl_ii[0] = bb[0] * tickers[0].length + bb[1];
                    cl_numm[0] = tickers[bb[0]][bb[1]][0];
                    cl_mem cl_i_buffer = clCreateBuffer(context,
                            CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                            Sizeof.cl_int, Pointer.to(cl_ii), null);
                    cl_mem cl_num_buffer = clCreateBuffer(context,
                            CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                            Sizeof.cl_double, Pointer.to(cl_numm), null);

                    clSetKernelArg(kernel_add, 0, Sizeof.cl_mem, Pointer.to(cltickers));
                    clSetKernelArg(kernel_add, 1, Sizeof.cl_mem, Pointer.to(cl_i_buffer));
                    clSetKernelArg(kernel_add, 2, Sizeof.cl_mem, Pointer.to(cl_num_buffer));

                    clEnqueueNDRangeKernel(commandQueue, kernel_add, 1, null,
                            new long[]{1}, null, 0, null, null);
                    clReleaseMemObject(cl_i_buffer);
                    clReleaseMemObject(cl_num_buffer);

                    cl_ii[0] = bb[1] * tickers[0].length + bb[0];
                    cl_numm[0] = tickers[bb[1]][bb[0]][0];
                    cl_i_buffer = clCreateBuffer(context,
                            CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                            Sizeof.cl_int, Pointer.to(cl_ii), null);
                    cl_num_buffer = clCreateBuffer(context,
                            CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                            Sizeof.cl_double, Pointer.to(cl_numm), null);

                    clSetKernelArg(kernel_add, 0, Sizeof.cl_mem, Pointer.to(cltickers));
                    clSetKernelArg(kernel_add, 1, Sizeof.cl_mem, Pointer.to(cl_i_buffer));
                    clSetKernelArg(kernel_add, 2, Sizeof.cl_mem, Pointer.to(cl_num_buffer));

                    clEnqueueNDRangeKernel(commandQueue, kernel_add, 1, null,
                            new long[]{1}, null, 0, null, null);
                    clReleaseMemObject(cl_i_buffer);
                    clReleaseMemObject(cl_num_buffer);

                }
                synchronized (lock) {
                    if (!lock) {
                        lock = true;
                        new Thread(() -> {

                            try {
                                run_calc();
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }).start();
                    }
                }

//                System.out.println("stop "+System.currentTimeMillis());
            }));
            stream_list = new ArrayList<>();
            connections_num++;
        }


        ArrayList<Integer> v = new ArrayList<>();
        v.add(3);
//        rec(v,999);
        rec(v);
        System.out.println(vays.size());
        time_of_plus = new long[vays.size()];


        for (int i = 0; i < tickers.length; i++) {
            for (int j = 0; j < tickers[i].length; j++) {
                tickers1d[tickers.length*i+j] = tickers[i][j][0];
            }
        }
        for (int i = 0; i < vays.size(); i++) {
            int l = vays.get(i).size();
            for (int j = 0; j < VAY_LEN-l; j++) {
                vays.get(i).add(vays.get(i).get(0));
            }
        }
        int[] vays1d = new int[vays.size()* vays.get(0).size()];
        for (int i = 0; i < vays.size(); i++) {
            for (int j = 0; j < vays.get(i).size(); j++) {
                vays1d[vays.get(0).size()*i+j] = vays.get(i).get(j);
            }
        }

        clvays = clCreateBuffer(context,
                CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                (long) Sizeof.cl_double * vays1d.length, Pointer.to(vays1d), null);
        clans = clCreateBuffer(context,
                CL_MEM_READ_WRITE,
                (long) Sizeof.cl_double * vays1d.length, null, null);

        int[] size = new int[]{allvalues.size(),vays.get(0).size()};

        sizes = clCreateBuffer(context,
                CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                (long) Sizeof.cl_int * size.length, Pointer.to(size), null);
        global_work_size = new long[]{vays.size()};
        Thread.sleep(3000);
        lock = false;
        System.out.println("free lock");
//        wsClient.tradeStream("BNBBTC", ((event) -> {
//            JSONObject o = new JSONObject(event);
//            System.out.println("GTEDGOUIYFG  "+System.currentTimeMillis()+" "+(System.currentTimeMillis()-o.getDouble("E")));
//        }));
        JSONObject params = new JSONObject();
        params.put("requestId", "randomId");
        params.put("returnRateLimits",false);
        while (true){
//            System.out.println(System.currentTimeMillis());
//            result = spotClient.createMarket().time();
//            System.out.println(result+" ww");
            wstradeclient.general().ping(params);
//            System.out.println(System.currentTimeMillis());
//            wstradeclient.general().serverTime(params);
//            System.out.println(System.currentTimeMillis());
            spotClient.createMarket().ping();

            Thread.sleep(30000);
        }
//        Thread.sleep(60000);
//        wsClient.closeAllConnections();
    }

}
