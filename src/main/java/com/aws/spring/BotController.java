package com.aws.spring;

import com.aws.spring.database.CloudSqlConnectionPullFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Controller
public class BotController {

    @Autowired
    LexService lex;

    @GetMapping("/")
    public String greetingForm(Model model) {
        return "index";
    }

    // Handles a string posted from the client.
    @RequestMapping(value = "/text", method = RequestMethod.POST)
    @ResponseBody
    String addItems(HttpServletRequest request, HttpServletResponse response) {

        String text = request.getParameter("text");
        String message = lex.getText(text);
        return message;
    }

    @GetMapping("/allData")
    public List<DataModel> getAllData() {
        List<DataModel> dataList = new ArrayList<>();
        DataSource dataSource = CloudSqlConnectionPullFactory.createConnectionPool();
        ResultSet rs =
                null;
        try {
            rs = dataSource.getConnection().prepareStatement("SELECT * FROM reservations").executeQuery();
            while (rs.next()) {
                dataList.add(new DataModel(rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDate("reservation_date"),
                        rs.getString("product_name"),
                        rs.getString("address")));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return dataList;
    }
}