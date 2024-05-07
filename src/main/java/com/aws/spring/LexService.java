package com.aws.spring;

import com.aws.spring.database.CloudSqlConnectionPullFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.comprehend.ComprehendClient;
import software.amazon.awssdk.services.comprehend.model.ComprehendException;
import software.amazon.awssdk.services.comprehend.model.DetectDominantLanguageRequest;
import software.amazon.awssdk.services.comprehend.model.DetectDominantLanguageResponse;
import software.amazon.awssdk.services.comprehend.model.DominantLanguage;
import software.amazon.awssdk.services.comprehend.model.LanguageCode;
import software.amazon.awssdk.services.lexruntimev2.LexRuntimeV2Client;
import software.amazon.awssdk.services.lexruntimev2.model.Message;
import software.amazon.awssdk.services.lexruntimev2.model.RecognizeTextRequest;
import software.amazon.awssdk.services.lexruntimev2.model.RecognizeTextResponse;
import software.amazon.awssdk.services.translate.TranslateClient;
import software.amazon.awssdk.services.translate.model.TranslateTextRequest;
import software.amazon.awssdk.services.translate.model.TranslateTextResponse;
import software.amazon.awssdk.services.translate.model.TranslateException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Component
public class LexService {
    public static final String BOT_ID = "RK7EQXXV0R";
    public static final String ALIAS_ID = "DDHGYEQYCU";
    public static final String SESSION_ID = UUID.randomUUID().toString();
    public static final String LOCALE_ID = "en_US";
    public static final String ACCESS_KEY = "AKIAU6GD26VN46CXGJWS";
    public static final String SECRET_KEY = "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";
    public static final Region REGION = Region.US_EAST_1;
    public static final AwsCredentialsProvider awsCredentialsProvider = StaticCredentialsProvider.create(AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY));
    String name;
    String product;
    String address;
    Date date;

    public String getText(String text) {
        String textLenCode = detectLanguage(text);
        if (!LanguageCode.knownValues().contains(LanguageCode.fromValue(textLenCode))) {
            return "Unkown language, please, write your answer again";
        }
        if (textLenCode != LanguageCode.EN.name()) {
            text = textTranslateToEn(textLenCode, text);
        }

        LexRuntimeV2Client lexV2Client = LexRuntimeV2Client
                .builder()
                .credentialsProvider(awsCredentialsProvider)
                .region(REGION)
                .build();

        RecognizeTextRequest recognizeTextRequest = RecognizeTextRequest.builder()
                .botAliasId(ALIAS_ID)
                .botId(BOT_ID)
                .localeId(LOCALE_ID)
                .sessionId(SESSION_ID)
                .text(text)
                .build();
        RecognizeTextResponse textResponse = lexV2Client.recognizeText(recognizeTextRequest);
        StringBuilder stringBuilder = new StringBuilder();
        for (Message message : textResponse.messages()) {
            stringBuilder.append(message.content());
        }
        if (stringBuilder.toString().equals(new String("What product do you want to order?"))) {
            name = text;
        }
        extractInformation(stringBuilder.toString());
        if (stringBuilder.toString().equals(new String("Your request is completed succesfully!"))) {
            System.out.println("ORDERED");
            addInfoInDb();
        }
        System.out.println(stringBuilder.toString());
        return stringBuilder.toString();
    }

    private String detectLanguage(String text) {
        ComprehendClient comClient = ComprehendClient.builder()
                .region(REGION)
                .credentialsProvider(awsCredentialsProvider)
                .build();

        try {
            String lanCode = "";
            DetectDominantLanguageRequest request = DetectDominantLanguageRequest.builder()
                    .text(text)
                    .build();

            DetectDominantLanguageResponse resp = comClient.detectDominantLanguage(request);
            List<DominantLanguage> allLanList = resp.languages();
            Iterator<DominantLanguage> lanIterator = allLanList.iterator();

            while (lanIterator.hasNext()) {
                DominantLanguage lang = lanIterator.next();
                lanCode = lang.languageCode();
            }

            return lanCode;

        } catch (ComprehendException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
        return "";
    }

    public String textTranslateToEn(String lanCode, String text) {
        TranslateClient translateClient = TranslateClient.builder()
                .credentialsProvider(awsCredentialsProvider)
                .region(REGION)
                .build();
        try {
            TranslateTextRequest textRequest = TranslateTextRequest.builder()
                    .sourceLanguageCode(lanCode)
                    .targetLanguageCode("en")
                    .text(text)
                    .build();

            TranslateTextResponse textResponse = translateClient.translateText(textRequest);
            return textResponse.translatedText();

        } catch (TranslateException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        return "";
    }

    private void extractInformation(String sentence) {
        Pattern productPattern = Pattern.compile("product ([\\w\\s\\d]+?) is reserved");
        Pattern addressPattern = Pattern.compile("at ([\\w\\s\\d]+?) to the date");
        Pattern datePattern = Pattern.compile("to the date (\\d{4}-\\d{2}-\\d{2}).  Shall I book the reservation?");

        // Match patterns in the sentence
        Matcher productMatcher = productPattern.matcher(sentence);
        Matcher addressMatcher = addressPattern.matcher(sentence);
        Matcher dateMatcher = datePattern.matcher(sentence);

        if (productMatcher.find() && addressMatcher.find() && dateMatcher.find()) {
            product = productMatcher.group(1);
            address = addressMatcher.group(1);
            String stringDate = dateMatcher.group(1);
            this.date = new Date(Integer.valueOf(
                    stringDate.split("-")[0]) - 1900,
                    Integer.valueOf(stringDate.split("-")[1]) - 1,
                    Integer.valueOf(stringDate.split("-")[2])
            );
        }
    }

    private void addInfoInDb() {
        String sql = "INSERT INTO reservations (name, reservation_date, product_name, address) " +
                "VALUES(?, ?, ?, ?)";
        DataSource dataSource = CloudSqlConnectionPullFactory.createConnectionPool();
        try (Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name);
            statement.setDate(2, date);
            statement.setString(3, product);
            statement.setString(4, address);
            int affectedRows = statement.executeUpdate();
            if (affectedRows < 1) {
                throw new RuntimeException("No rows inserted");
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}