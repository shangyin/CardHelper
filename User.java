package CardHelper;

import org.apache.http.conn.ConnectTimeoutException;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import javax.jws.soap.SOAPBinding;
import java.io.*;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Created by 41237 on 2016/10/22.
 */
public class User
{
    private String name;
    private String password;
    private String email;
    private LocalDate localDate;
    private LocalTime localTime;

    public String getName()
    {
        return name;
    }

    public String getPassword()
    {
        return password;
    }

    public String getEmail()
    {
        return email;
    }

    public LocalDate getLocalDate() {
        return localDate;
    }

    public LocalTime getLocalTime() {
        return localTime;
    }

    public User(String name, String password, String email, String date, String time) {
        this.name = name;
        this.password = password;
        this.email = email;
        DateTimeFormatter formatter = date.contains("-") ? DateTimeFormatter.ISO_DATE
                : DateTimeFormatter.BASIC_ISO_DATE;
        this.localDate = LocalDate.parse(date, formatter);
        this.localTime = LocalTime.parse(time, DateTimeFormatter.ISO_LOCAL_TIME);
    }

    /**
     * 返回指定发送时间的用户信息
     * @param from 时间开始区间
     * @param to 时间结束区间
     * @return
     * @throws Exception
     */
    public static List<User> researchUsers(LocalTime from, LocalTime to) throws Exception
    {
        List<User> users = new ArrayList<>();

        Class.forName("com.mysql.jdbc.Driver");
        Connection connection = DriverManager.getConnection("jdbc:mysql://localhost/card_helper", "root", "123456");
        PreparedStatement statement = connection.prepareStatement("select * from user where send_time between ? and ?");
        statement.setTime(1, Time.valueOf(from));
        statement.setTime(2, Time.valueOf(to));
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next())
        {
            String id = resultSet.getString("id");
            String password = resultSet.getString("password");
            String email = resultSet.getString("email");
            String date = resultSet.getDate("sign_up").toString();
            String time = resultSet.getTime("send_time").toString();
            users.add(new User(id, password, email, date, time));
        }
        resultSet.close();
        statement.close();
        connection.close();
        return users;
    }

    public static void updateUser(User user) throws Exception
    {
        Class.forName("com.mysql.jdbc.Driver");
        Connection connection = DriverManager.getConnection("jdbc:mysql://localhost/card_helper", "root", "1234567890Abc");
        PreparedStatement statement = connection.prepareStatement("insert into user (id, password, email, sign_up, send_time) values (?, ?, ?, ?, ?)");
        statement.setString(1, user.getName());
        statement.setString(2, user.getPassword());
        statement.setString(3, user.getEmail());
        statement.setString(4, user.getLocalDate().toString());
        statement.setString(5, user.getLocalTime().toString());
        statement.executeUpdate();
        statement.close();
        connection.close();
    }

    @Override
    /**
     * 返回值信息不完整，仅用于测试
     */
    public String toString()
    {
        return "name:" + name + "\tpassword: " + password + "\temail: " + email;
    }

    /**
     * @deprecated 从user.xml获取用户信息
     * @param file 要读取的user.xml
     * @return 用户列表，没有过滤
     * @throws Exception XML读取问题
     */
    public static List<User> parseUsers(String file) throws Exception
    {
        File xml = new File(file);
        SAXReader reader = new SAXReader();
        org.dom4j.Document doc = reader.read(xml);
        List<Element> elements = doc.getRootElement().elements();

        return elements.stream()
                .map(x -> new User(x.element("name").getText(),
                        x.element("password").getText(),
                        x.element("email").getText(),
                        x.element("date").getText(),
                        x.element("time").getText()))
                .collect(Collectors.toList());
    }

    /**
     * @deprecated
     * @param file 保存User的文件
     * @param user 要保存的User对象
     * @throws Exception
     */
    public static void saveUser(String file, User user) throws Exception
    {
        SAXReader sr = new SAXReader();
        org.dom4j.Document doc = sr.read(new File(file));

        Element element = doc.getRootElement().addElement("user");
        element.addElement("name").setText(user.name);
        element.addElement("password").setText(user.password);
        element.addElement("email").setText(user.email);
        element.addElement("date").setText(user.localDate.format(DateTimeFormatter.BASIC_ISO_DATE));
        element.addElement("time").setText(user.localTime.format(DateTimeFormatter.ISO_LOCAL_TIME));

        OutputFormat format = OutputFormat.createPrettyPrint();
        format.setEncoding("utf-8");
        XMLWriter writer = new XMLWriter(new FileWriter(new File(file)), format);
        writer.write(doc);
        writer.close();
    }

    /**
     * @deprecated
     * 用于新增用户
     * @param file 保存用户的xml文件
     * @throws Exception
     */
    public static void input(String file) throws Exception
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("学号:");
        String name = reader.readLine();
        System.out.print("密码:");
        String password = reader.readLine();
        System.out.print("邮箱：");
        String email = reader.readLine();
        System.out.print("几点发邮件（0-23）：");
        String time = reader.readLine();
        User user = new User(name, password, email, LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE), time);
        saveUser(file, user);
    }


}

