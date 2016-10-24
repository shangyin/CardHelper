package CardHelper;

import org.apache.commons.mail.SimpleEmail;
import org.apache.http.Header;

import java.time.LocalDate;
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

    public SendMail(String name, String psw)
    {
        this.hostName = name;
        this.hostPsw = psw;
    }

    public static void main(String[] args) throws Exception
    {
        List<User> users = User.parseUsers("user.xml");
        SendMail host = new SendMail("", "");

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
                //查询一卡通日期为昨天
                LocalDate target = LocalDate.now().minusDays(1);
                String date = target.format(DateTimeFormatter.BASIC_ISO_DATE);
                List<Record> records = cardCenter.getRecords(date, date);
                cardInfo.close();
                //发送邮件
                String content = generateContent(records);
                mail.send(user.getEmail(), "昨日消费", content);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private static String generateContent(List<Record> records)
    {
        StringBuilder sb = new StringBuilder();
        //消费概括
        sb.append("共消费：" + Record.addUpConsume(records) + separator);
        if (records.size() == 0) {
            sb.append("今天没使用饭卡消费！");
        } else {
            sb.append("当前余额为：" + records.get(0).getTotal());
        }
        sb.append(separator);
        //具体消费条目
        String content = records.stream()
                .map( x -> "消费：" + x.getAmount() + " " + x.getPlace() + " " + x.getTime())
                .collect(Collectors.joining(separator, separator, separator));
        sb.append(content);
        return sb.toString();
    }

    public void send(String targetAdr, String title, String content) throws Exception
    {
        SimpleEmail email = new SimpleEmail();
        email.setSSLOnConnect(true);
        email.setCharset("utf-8");
        email.setHostName("smtp.163.com");
        email.setAuthentication(hostName, hostPsw);
        email.setFrom(hostName);
        email.addTo(targetAdr);
        email.setSubject(title);
	    email.setMsg(content);
        email.send();
    }
}