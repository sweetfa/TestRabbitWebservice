package au.com.bushlife.integration.test.utils.rabbit;

import static java.text.MessageFormat.format;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileDumpingService {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    private final Map<Key, BiConsumer<Context, MultipartFile>> processors = new HashMap<>() {
        {
            put(new Key("application/json", "text"), this::jsonProcessor);
            put(new Key("multipart/form-data", "text"), this::fileProcessor);
            put(new Key("multipart/form-data", "fixed-binary"), this::binaryProcessor);
        }

        private void binaryProcessor(Context context, MultipartFile multipartFile) {
            try {
                if (context.length == 0) {
                    throw new RuntimeException("Length required for fixed-binary file processing");
                }
                DataInputStream is = new DataInputStream(multipartFile.getInputStream());
                byte[] result;
                int cnt = 0;
                while ((result = is.readNBytes(context.length)).length > 0) {
                    rabbitTemplate.convertAndSend(context.queueName, result);
                    ++cnt;
                }
                log.info(format("Sent {0} records", cnt));
                is.close();
            } catch (IOException e) {
                log.error("exception converting file to stream", e);
            }
        }

        private void fileProcessor(Context context, MultipartFile multipartFile) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(multipartFile.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    rabbitTemplate.convertAndSend(context.queueName, line);
                }
                reader.close();
            } catch (IOException e) {
                log.error("exception converting file to multiple lines", e);
            }
        }

        private void jsonProcessor(Context context, MultipartFile multipartFile) {
            try {
                rabbitTemplate.convertAndSend(context.queueName, objectMapper.writeValueAsString(multipartFile.getInputStream()));
            } catch (IOException e) {
                log.error("exception converting file to json object", e);
            }
        }
    };

    public void dumpFile(String queueName, List<String> contentTypes, String mode, int len, MultipartFile file) {
        Context context = new Context(queueName, mode, len);
        contentTypes.stream()
            .map(contentType -> contentType.split(";")[0])
            .filter(Objects::nonNull)
            .filter(ct -> processors.containsKey(new Key(ct, mode)))
            .forEach(contentType -> processors.get(new Key(contentType, mode))
                .accept(context, file));
    }

    @Data
    private class Key {
        private final String contentType;
        private final String mode;
    }

    @Data
    private class Context {
        private final String queueName;
        private final String mode;
        private final int length;
    }
}
