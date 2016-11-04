package CardHelper;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Created by 41237 on 2016/10/24.
 */
public class CardCenter
{

    private final CloseableHttpClient client;
    private final String user;
    private final String password;
    private final String cardId;

    public CardCenter(XinXiMenHu xinXiMenHu) throws Exception
    {
        this.client = xinXiMenHu.client;
        this.user = xinXiMenHu.user;
        this.password = xinXiMenHu.password;

        client.execute(new HttpGet("http://cardinfo.gdufe.edu.cn/gdcjportalHome.action")).close();
        this.cardId = getCardId();
//        client.execute(new HttpGet("http://cardinfo.gdufe.edu.cn/accleftframe.action")).close();
//        client.execute(new HttpGet("http://cardinfo.gdufe.edu.cn/mainFrame.action")).close();
    }


    public List<Record> getRecords(String fromDate, String toDate) throws Exception
    {
        submitId(cardId);
        submitDate(fromDate, toDate);
        return parseAllPages(getRecordHtml(), fromDate, toDate);
    }

    public List<Record> getRecords(String date) throws Exception
    {
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        if (today.equals(date)) {
            return getToday();
        } else {
            return getRecords(date, date);
        }
    }

    private List<Record> getToday() throws Exception
    {
        //获取当日记录第一页
        HttpPost post = new HttpPost("http://cardinfo.gdufe.edu.cn/accounttodatTrjnObject.action");
        List<NameValuePair> args = new LinkedList<>();
        args.add(new BasicNameValuePair("account", cardId));
        args.add(new BasicNameValuePair("inputObject", "all"));
        post.setEntity(new UrlEncodedFormEntity(args));
        CloseableHttpResponse response = client.execute(post);
        String html = Utils.getContent(response);
        response.close();
        //解析当日记录所有页
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        return parseAllPages(html, date, date);
    }


    /**
     * 解析一卡通系统的用户信息页面，以获得用户饭卡ID
     * @param s 一卡通的信息页面
     * @return 饭卡ID
     */
    private static String parseId(String s)
    {
        Document doc = Jsoup.parse(s);
        Element element = doc.getElementById("account");
        return element.child(0).attr("value");
    }


    private String getRecordHtml() throws IOException
    {
        HttpPost post = new HttpPost("http://cardinfo.gdufe.edu.cn/accounthisTrjn3.action");
        CloseableHttpResponse response = client.execute(post);
        String html = Utils.getContent(response);
        response.close();
        return html;
    }

    private void submitDate(String fromDate, String toDate) throws IOException
    {
        HttpPost post = new HttpPost("http://cardinfo.gdufe.edu.cn/accounthisTrjn2.action");
        List<NameValuePair> args = new ArrayList<>();
        args.add(new BasicNameValuePair("inputStartDate", fromDate));
        args.add(new BasicNameValuePair("inputEndDate", toDate));
        post.setEntity(new UrlEncodedFormEntity(args));
        CloseableHttpResponse response = client.execute(post);
        response.close();
    }

    private void submitId(String cardId) throws IOException
    {
        HttpPost post = new HttpPost("http://cardinfo.gdufe.edu.cn/accounthisTrjn1.action");
        List<NameValuePair> args = new ArrayList<>();
        args.add(new BasicNameValuePair("account", cardId));
        args.add(new BasicNameValuePair("inputObject", "all"));
        post.setEntity(new UrlEncodedFormEntity(args));
        CloseableHttpResponse response = client.execute(post);
        response.close();
    }

    /**
     *
     * @return 饭卡ID
     * @throws IOException
     */
    private String getCardId() throws IOException
    {
        HttpGet get = new HttpGet("http://cardinfo.gdufe.edu.cn/accounthisTrjn.action");
        CloseableHttpResponse response =  client.execute(get);
        String s = Utils.getContent(response);
        response.close();
        return parseId(s);
    }

    private static List<Record> parseSinglePage(String html)
    {
        Document doc = Jsoup.parse(html);
        Elements elements1 =  doc.getElementById("tables").children().get(0).children();
        //tables标签下第一个和最后一个标签分别是属性名和页数信息
        //TODO use subList
        elements1.remove(elements1.size() - 1);
        elements1.remove(0);
        List<Record> ret = new ArrayList<>(elements1.size());
        for (Element e : elements1)
        {
            //e.child(0) 的text 样式为 2016/10/10 14:10:10
            int[] args = Arrays.stream(e.child(0).text().split("\\s"))
                    .flatMap(s -> Arrays.stream(s.split("[/:]")))
                    .mapToInt(Integer::parseInt)
                    .toArray();
            LocalDateTime dt = LocalDateTime.of(args[0], args[1], args[2], args[3], args[4], args[5]);
            String type = e.child(3).text();    //条目类型，例如持卡人消费
            String place = e.child(4).text().trim();    //消费地点
            BigDecimal amount = new BigDecimal(e.child(5).text());  //数额
            BigDecimal total = new BigDecimal(e.child(6).text());   //余额
            int count = Integer.parseInt(e.child(7).text());    //刷卡次数
            String state = e.child(8).text();   //状态
            //还有说明项

            Record.RecordBuilder builder = new Record.RecordBuilder(amount, total, dt.toLocalDate(), dt.toLocalTime());
            builder.setCount(count)
                    .setPlace(place)
                    .setType(type)
                    .setState(state);
            Record record = new Record(builder);
            ret.add(record);
        }
        return ret;
    }


    private List<Record> parseAllPages(String html, String from, String to)
    {
        List<Record> ret = new ArrayList<>();
        ret.addAll(parseSinglePage(html));     //处理第一页

        //获取最后tables的最后一栏，里面有当前页和总页数
        Document doc = Jsoup.parse(html);
        Element element = doc.getElementsByClass("bl").get(1).child(0).child(0);
        String pageCount = element.text();

        //获取数字
        Pattern p = Pattern.compile("(\\d*?)(?=页)", Pattern.DOTALL);
        Matcher m = p.matcher(pageCount);
        List<Integer> result = new ArrayList<>(2);
        while (m.find()) {
            String tmp = m.group();
            if (!tmp.equals("")) {
                result.add(Integer.parseInt(tmp));
            }
        }
        int totalPage = result.get(0) > result.get(1) ? result.get(0) : result.get(1);
        for (int i = 2; i <= totalPage; i++)
        {
            //请求其它页面
            HttpPost post = new HttpPost("http://cardinfo.gdufe.edu.cn/accountconsubBrows.action");
            List<BasicNameValuePair> args = new ArrayList<>(3);
            args.add(new BasicNameValuePair("inputStartDate", from));
            args.add(new BasicNameValuePair("inputEndDate", to));
            args.add(new BasicNameValuePair("pageNum", "" + i));
            try {
                post.setEntity(new UrlEncodedFormEntity(args));
                CloseableHttpResponse response = client.execute(post);
                String page = Utils.getContent(response);
                ret.addAll(parseSinglePage(page));  //整合数据
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    public void close() throws Exception
    {
        client.close();
    }
}
