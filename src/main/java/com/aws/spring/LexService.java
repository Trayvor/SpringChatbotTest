package com.aws.spring;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
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
import software.amazon.awssdk.services.lexruntimev2.model.LexRuntimeV2Exception;
import software.amazon.awssdk.services.lexruntimev2.model.Message;
import software.amazon.awssdk.services.lexruntimev2.model.RecognizeTextRequest;
import software.amazon.awssdk.services.lexruntimev2.model.RecognizeTextResponse;
import software.amazon.awssdk.services.translate.TranslateClient;
import software.amazon.awssdk.services.translate.model.TranslateTextRequest;
import software.amazon.awssdk.services.translate.model.TranslateTextResponse;
import software.amazon.awssdk.services.translate.model.TranslateException;

import java.util.*;


@Component
public class LexService {
    public static final String BOT_ID = "X00GHWINE4";
    public static final String ALIAS_ID = "BLSHM29LOD";
    public static final String SESSION_ID = UUID.randomUUID().toString();
    public static final String LOCALE_ID = "en_US";
    public static final String ACCESS_KEY = "AKIAW3MEDJMNB5D76AG4";
    public static final String SECRET_KEY = "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";
    public static final Region REGION = Region.US_EAST_1;

    public String getText(String text) {
        String textLenCode = detectLanguage(text);
        if (textLenCode != LanguageCode.EN.name()) {
            text = textTranslateToEn(textLenCode, text);
        }
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY);
        AwsCredentialsProvider awsCredentialsProvider = StaticCredentialsProvider.create(awsCreds);

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
        return stringBuilder.toString();
    }

    private String detectLanguage(String text) {
        Region region = Region.US_EAST_1;
        ComprehendClient comClient = ComprehendClient.builder()
                .region(region)
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

        Region region = Region.US_EAST_1;
        TranslateClient translateClient = TranslateClient.builder()
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .region(region)
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
}