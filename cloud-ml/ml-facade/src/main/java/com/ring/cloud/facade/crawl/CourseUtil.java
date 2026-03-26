package com.ring.cloud.facade.crawl;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.ring.cloud.facade.util.FileUtil;
import com.ring.cloud.facade.util.WebUtil;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 抓取赛程表
 */
@Slf4j
public class CourseUtil {
    public static final long wait_home = 3*1000;
    public static final long wait_page = 2*1000;

    public static void main(String[] args) throws IOException {
        String scUrl = "https://odds.500.com/";
        String scDXUrl = "https://odds.500.com/daxiao_jczq.shtml";
        String sfUrl = "https://odds.500.com/index_sfc.shtml";
        String sfDXUrl = "https://odds.500.com/daxiao_sfc.shtml";
        String sdDXUrl = "https://odds.500.com/daxiao_zqdc.shtml";//单场大小球
        String name = "赛程";
        if(args.length>1){
            sfUrl = args[0];
            sfDXUrl = args[1];
        }
        System.out.println("欢迎 !");
        System.out.println("苍穹系统-------------------------------------");
        System.out.println("-----------");
        System.out.println("-----------------------");
        System.out.println("-----------");
        System.out.println("-----------------------");
        System.out.println("-----------");
        System.out.println("-----------------------");
        System.out.println("===============================================>");
        long start = System.currentTimeMillis();
        Map<String, String> sfMap = crawlBigBoll(sfDXUrl);//从胜负场中获取大小指数
        Map<String, String> sessionMap = crawlBigBoll(sdDXUrl);//从单场中获取所有大小指数
        long end1 = System.currentTimeMillis();
        System.out.println("ds-----------------------"+(end1-start)+" ms");
//        List<SEntity> scList = crawlCourse(scUrl, crawlBigBoll(scDXUrl));//赛程
        List<SEntity> scList = crawlCourse(scUrl, sfMap, sessionMap);//赛程
        long end2 = System.currentTimeMillis();
        System.out.println("sc-------------------"+(end2-end1)+" ms");
        List<SEntity> sfList = crawlCourse(sfUrl, sfMap, sessionMap);//胜负
//        List<SEntity> sfList = crawlCourse(sfUrl, sessionMap);//胜负
        long end3 = System.currentTimeMillis();
        System.out.println("sf-------------------"+(end3-end2)+" ms");
        List<String> scKeys = scList.stream().map(SEntity::getKey).collect(Collectors.toList());
        List<String> sfKeys = sfList.stream().map(SEntity::getKey).collect(Collectors.toList());
        //过滤赛程不包含的胜负场
        List<SEntity> sfFilteredList = sfList.stream()
                .filter(se -> !scKeys.contains(se.getKey()))
                .collect(Collectors.toList());
        //标记赛程包含的胜负场
        List<SEntity> scListFilter = new ArrayList<>();
        List<SEntity> scfList = new ArrayList<>();
                scList.forEach(sc->{
            String key = sc.getKey();
            if(sfKeys.contains(sc.getKey())){
                String newInfo = sc.getInfo().replaceAll(key, "*"+key);
                sc.setInfo(newInfo); //场次加星
                scfList.add(sc);
            }else
                scListFilter.add(sc);
        });
        StringBuilder scf = build(scfList);//赛程
        StringBuilder sc = build(scListFilter);//赛程
        StringBuilder sf = build(sfFilteredList);//胜负
        scf.append(System.lineSeparator());
        scf.append("14场").append(System.lineSeparator()).append(sf).append(System.lineSeparator()).append(sc);
        write(scf.toString(), name);
        System.out.println("xr-----------------------"+(System.currentTimeMillis()-end3)+" ms");
    }

    //输出字符串
    public static StringBuilder build(List<SEntity> sList) {
        StringBuilder str = new StringBuilder();
        sList.forEach(s -> str.append(s.getInfo()).append(System.lineSeparator()));
        return str;
    }

    /** 让球
     * 0=周一001
     * 1=意甲
     * 2=第30轮
     * 3=04-01 00:30
     * 4=维罗纳
     * 5=VS
     * 6=帕尔马
     * 7=Bet365
     * 8=1.100
     * 9=平手/半球
     * 10=0.810
     * 11=2.45
     * 12=3.10
     * 13=3.10
     * 14=94.9%
     * 15=析 亚 欧 情
     * @param url
     */
    public static List<SEntity> crawlCourse(String url, Map<String, String> sfMap, Map<String, String> sessionMap) {
        WebClient webClient = null;
        List<SEntity> list = new ArrayList<>();
        try{
            webClient = WebUtil.getClient(url);
            HtmlPage page = webClient.getPage(url);
            webClient.waitForBackgroundJavaScript(wait_home);
            String pageAsXml = page.asXml();
//            FileUtil.write(pageAsXml);
            Document doc = Jsoup.parse(pageAsXml, url);
            Element data = doc.getElementById("main-tbody");
//            List<CourseEntity> courses = new ArrayList<>();
            assert data != null;
            Elements trs = data.select("tr");
            for(Element tr:trs){
                Elements tds = tr.select("td");
                if(tds.size()<10) continue;
                StringBuilder str = new StringBuilder();
                String record = tds.get(0).text();//场次
                if(record.length()>3){
                    String shortRecord = record.substring(record.length()-2);
                    if(list.size()>0 && "01".equals(shortRecord))
                        str.append(System.lineSeparator());
                    str.append(shortRecord).append(" ");
                }
                String duiZhen = tds.get(4).text()+"-"+tds.get(6).text();
                str.append(duiZhen).append(" ");//对阵
                String letC = tds.get(9).text();//让球
                String letD = CommonLet.letMap.getOrDefault(letC, " ");
                String success = tds.get(8).text();
                String fail = tds.get(10).text();
                String ds = sfMap.containsKey(duiZhen)? sfMap.get(duiZhen):sessionMap.getOrDefault(duiZhen, "");
                str.append(letD).append(compare(success, fail)).append("  ").append(ds).append("  ");//让球和大小球
                list.add(SEntity.builder().key(duiZhen).info(str.toString()).build());
            }
        }catch (Throwable e){
            log.error("抓取让球失败，url: "+url+e.getMessage());
        }finally {
            if(webClient!=null)
                webClient.close();//关闭窗口，释放内存
        }
        return list;
    }

    /** 大小球
     * 周一001
     * 意甲
     * 第30轮
     * 04-01 00:30
     * 维罗纳
     * VS
     * 帕尔马
     * 0.89
     * 1.00
     * 0.84
     * 0.95
     * 0.89
     * 析 亚 欧 情
     * 2/2.5(球)
     * 2.5(球)
     * 2/2.5(球)
     * 2.5(球)
     * 2/2.5(球)
     * 1.00
     * 0.80
     * 0.96
     * 0.75
     * 0.99
     * @return
     */
    public static Map<String, String> crawlBigBoll(String url) {
    Map<String, String> map = new HashMap<>();
    WebClient webClient = null;
    StringBuilder str = new StringBuilder();
    try{
        webClient = WebUtil.getClient(url);
        HtmlPage page = webClient.getPage(url);
        webClient.waitForBackgroundJavaScript(wait_home);
        String pageAsXml = page.asXml();
            FileUtil.write(pageAsXml);
        Document doc = Jsoup.parse(pageAsXml, url);
        Element data = doc.getElementById("main-tbody");
        Elements trs = data.select("tr");
        int count = 0;
        String key = "";
        String big = "";
        String up = "";
        String down = "";
        for(Element tr:trs){
            count++;
            Elements tds = tr.select("td");
            if(count==1){
                key = tds.get(4).text()+"-"+tds.get(6).text();//场次;
                up = tds.get(8).text();
            }else if(count==2){
                big = tds.get(1).text();
                big = formatBigData(big);//转换大小球格式
            }else {
                down = tds.get(1).text();
                map.put(key, big+compare(up,down));
                count=0;
            }
        }
    }catch (Throwable e){
        log.error("抓取大小球失败，url: "+url+e.getMessage());
    }finally {
        if(webClient!=null)
            webClient.close();//关闭窗口，释放内存
    }
    return map;
}
    //转换大小球格式
    public static String formatBigData(String big) {
        if(big.contains("球"))
            big = big.substring(0, big.length()-3);
        if(!big.contains("/")) return big;
        BigDecimal bigNum = toDec(big.split("/")[0]);
        bigNum = bigNum.add(new BigDecimal("0.25")).setScale(1, RoundingMode.HALF_DOWN);
        return String.valueOf(bigNum);
    }
    //比较指数大小，大+小-
    public static String compare(String up, String down) {
        BigDecimal upNum = toDec(up);
        BigDecimal downNum = toDec(down);
        if(upNum.compareTo(downNum) < 0)
            return "a";
        return "";
    }

    //水位转float，保留两位数字
    public static BigDecimal toDec(String dataStr) {
        try {
            float num = Float.parseFloat(dataStr);
            BigDecimal bd = new BigDecimal(Float.toString(num));
            return bd.setScale(2, RoundingMode.HALF_UP); // 四舍五入到两位小数
        }catch (Throwable e){
            log.error("转数字失败。。。"+e.getMessage());
            return new BigDecimal(0);
        }
    }
    public static void write(String content, String name) {
        File file = null;
        PrintWriter pw = null;
        BufferedWriter bw = null;
        try {
            String fileName = "C://Users/cloud/Desktop/"+name+".txt";
            file = new File(fileName);
            if (!file.exists())
                file.createNewFile();
            else{
                file.delete();
                file = new File(fileName);
                if (!file.exists())
                    file.createNewFile();
            }
            pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "GBK"));
            bw = new BufferedWriter(pw);
            bw.write(content);
            bw.flush();
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (pw != null)
                    pw.close();
                if (bw != null)
                    bw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
