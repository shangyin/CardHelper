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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static CardHelper.Utils.getContent;

/**
 * Created by 41237 on 2016/10/24.
 */
public class CardCenter
{

    private final CloseableHttpClient client;
    private final String user;
    private final String password;

    public CardCenter(XinXiMenHu xinXiMenHu) {
        this.client = xinXiMenHu.client;
        this.user = xinXiMenHu.user;
        this.password = xinXiMenHu.password;
    }

    private void loginCard() throws IOException
    {
        client.execute(new HttpGet("http://cardinfo.gdufe.edu.cn/gdcjportalHome.action")).close();
//        client.execute(new HttpGet("http://cardinfo.gdufe.edu.cn/accleftframe.action")).close();
//        client.execute(new HttpGet("http://cardinfo.gdufe.edu.cn/mainFrame.action")).close();
    }

    private static String parseId(String s)
    {
        Document doc = Jsoup.parse(s);
        Element element = doc.getElementById("account");
        return element.child(0).attr("value");
    }

    public List<Record> getRecords(String fromDate, String toDate) throws Exception {
        loginCard();
        submitId(getCardId());
        submitDate(fromDate, toDate);
        return parseAllPages(getRecordHtml(), fromDate, toDate);
    }

    private String getRecordHtml() throws IOException
    {
        HttpPost post = new HttpPost("http://cardinfo.gdufe.edu.cn/accounthisTrjn3.action");
        CloseableHttpResponse response = client.execute(post);
        return getContent(response);
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

    private String getCardId() throws IOException
    {
        HttpGet get = new HttpGet("http://cardinfo.gdufe.edu.cn/accounthisTrjn.action");
        CloseableHttpResponse response =  client.execute(get);
        String s = getContent(response);
        return parseId(s);
    }

    private static List<Record> parseSinglePage(String html)
    {
        Document doc = Jsoup.parse(html);
        Elements elements1 =  doc.getElementById("tables").children().get(0).children();
        elements1.remove(elements1.size() - 1);
        elements1.remove(0);
        List<Record> ret = new ArrayList<>(elements1.size());
        for (Element e : elements1)
        {
            int[] args = Arrays.stream(e.child(0).text().split("\\s"))
                    .flatMap(s -> Arrays.stream(s.split("[/:]")))
                    .mapToInt(Integer::parseInt)
                    .toArray();
            LocalDateTime dt = LocalDateTime.of(args[0], args[1], args[2], args[3], args[4], args[5]);
            String type = e.child(3).text();
            String place = e.child(4).text().trim();
            BigDecimal amount = new BigDecimal(e.child(5).text());
            BigDecimal total = new BigDecimal(e.child(6).text());
            int count = Integer.parseInt(e.child(7).text());
            String state = e.child(8).text();

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
        ret.addAll(parseSinglePage(html));

        Document doc = Jsoup.parse(html);
        Element element = doc.getElementsByClass("bl").get(1).child(0).child(0);
        String pageCount = element.text();

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
            HttpPost post = new HttpPost("http://cardinfo.gdufe.edu.cn/accountconsubBrows.action");
            List<BasicNameValuePair> args = new ArrayList<>(3);
            args.add(new BasicNameValuePair("inputStartDate", from));
            args.add(new BasicNameValuePair("inputEndDate", to));
            args.add(new BasicNameValuePair("pageNum", "" + i));
            try {
                post.setEntity(new UrlEncodedFormEntity(args));
                CloseableHttpResponse response = client.execute(post);
                String page = getContent(response);
                response.close();
                ret.addAll(parseSinglePage(page));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return ret;
    }
}