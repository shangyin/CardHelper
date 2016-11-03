package CardHelper;

import org.apache.commons.mail.SimpleEmail;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by 41237 on 2016/10/22.
 */
public class SendMail
{
    public static final String separator = System.lineSeparator();
    private final String hostName;
    private final String hostPsw;

    private static String ps;

    public SendMail(String name, String psw)
    {
        this.hostName = name;
        this.hostPsw = psw;
    }

    public static void main(String[] args) throws Exception
    {
        SAXReader saxReader = new SAXReader();
        Document doc = saxReader.read(new File(args[0]));
        Element root = doc.getRootElement();
        SendMail host = new SendMail(root.elementText("email"), root.elementText("password"));
        ps = root.elementText("ps");

        LocalTime from = LocalTime.now().withMinute(0).withSecond(0).withNano(0);
        LocalTime to = from.plusHours(1);
        List<User> users = User.researchUsers(from, to);
        for (User u : users) {
            new Thread(new Handler(u, host)).start();
        }
    }

    private static class Handler implements Runnable
    {
        private User user;
        private SendMail mail;
        private Handler(User user, SendMail mail)
        {
            this.user = user;
            this.mail = mail;
        }

        public void run()
        {
            try
            {
                //登录信息门户
                XinXiMenHu cardInfo = new XinXiMenHu(user.getName(), user.getPassword());
                CardCenter cardCenter = new CardCenter(cardInfo);
                //查询一卡通日期，20点以前查询昨日
                List<Record> records;
                if (user.getLocalTime().getHour() < 20) {
                    String date = LocalDate.now().minusDays(1).format(DateTimeFormatter.BASIC_ISO_DATE);
                    records = cardCenter.getRecords(date, date);
                } else {
                    records = cardCenter.getToday();
                }
                cardInfo.close();
                //发送邮件
                String content = generateContent(records, ps);
                StringBuilder title = new StringBuilder();
                title.append("共消费：" + records.stream().mapToDouble(x -> x.getAmount().doubleValue()).sum());
                if (records.size() != 0) {
                    title.append(" 余额：" + records.get(0).getTotal().doubleValue());
                } else {
                    title.append(" 没有记录");
                }
                mail.send(user.getEmail(), title.toString(), content);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    static String generateContent(List<Record> records, String ps)
    {
        StringBuilder sb = new StringBuilder();
        //具体消费条目
        String consume = records.stream()
                .filter(x -> x.getType() == Record.TYPE.CONSUME)
                .map( x -> x.getAmount() + " " + x.getPlace() + " " + x.getTime())
                .collect(Collectors.joining(separator));
        String other = records.stream()
                .filter(x -> x.getType() == Record.TYPE.UNKNOWN)
                .map(x -> x.getAmount() + " " + x.getsType() + " " + x.getPlace() + " " + x.getTime())
                .collect(Collectors.joining(separator));
        sb.append("消费：" + separator);
        sb.append(consume);
        sb.append(separator + separator);
        sb.append("其它交易：" + separator);
        sb.append(other);
        sb.append(separator + separator);
        sb.append(ps);
        return sb.toString();
    }

    public void send(String targetAdr, String title, String content) throws Exception
    {
        SimpleEmail email = new SimpleEmail();
        email.setSSLOnConnect(true);
        email.setCharset("utf-8");
        email.setHostName("smtp.163.com");
        email.setAuthentication(hostName, hostPsw);
        email.setFrom(hostName, "一卡通助手");
        email.addTo(targetAdr);
        email.setSubject(title);
	    email.setMsg(content);
        email.send();
    }
}
