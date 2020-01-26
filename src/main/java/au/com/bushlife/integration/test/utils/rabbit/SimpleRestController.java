package au.com.bushlife.integration.test.utils.rabbit;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController("/rabbit")
@RequiredArgsConstructor
public class SimpleRestController {

    private final RabbitTemplate template;
    private final FileDumpingService fileDumpingService;

    @PostMapping("/rabbit/send")
    public ResponseEntity<String> postToNamedQueueAndWait(
        @RequestParam(value = "writeQueueName", required = true) String writeQueueName,
        @RequestParam(value = "readQueueName", required = true) String readQueueName,
        @RequestBody String queryIn,
        @RequestHeader HttpHeaders headers) {

      template.convertAndSend(writeQueueName, queryIn);
      Message response = template.receive(readQueueName, 60000);
      if (response == null) {
        return new ResponseEntity<>(HttpStatus.I_AM_A_TEAPOT);
      }
      HttpHeaders outHeaders = new HttpHeaders();
      return ResponseEntity.ok().headers  (new HttpHeaders(outHeaders)).body(new String(response.getBody()));
    }

//    private List<String> exclusionHeaders = new ArrayList<>() {
//        {
//            add("content-length");
//            add("Accept");
//            add("User-Agent");
//            add("Connection");
//            add("Postman-Token");
//            add("Host");
//            add("cache-control");
//            add("accept-encoding");
//            add("Content-Type");
//        }
//    };

//    private Map<String, String> filterHeaders(HttpHeaders headers) {
//        var result = headers.toSingleValueMap().entrySet().stream()
//            .filter(v -> !exclusionHeaders.contains(v.getKey()))
//            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
//        return result;
//    }

    @PostMapping("/rabbit/writetoqueue")
    public ResponseEntity<Void> postToNamedQueue(
        @RequestParam(value = "queueName", required = true) String queueName,
        @RequestBody String queryIn,
        @RequestHeader HttpHeaders headers) {

        template.convertAndSend(queueName, queryIn);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/rabbit/writetoexchange")
    public ResponseEntity<Void> postToNamedQueue(
        @RequestParam(value = "exchangeName", required = true) String exchangeName,
        @RequestParam(value = "routingKey", required = true) String routingKey,
        @RequestBody String queryIn,
        @RequestHeader HttpHeaders headers) {

        template.convertAndSend(exchangeName, routingKey, queryIn);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/monitor")
    public ResponseEntity<Void> drainQueue(
        @RequestParam(value = "queueName", required = true) String queueName,
        @RequestHeader HttpHeaders headers) {

        new AsyncMonitorLogger(template).monitor(queueName);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/rabbit/uploadToQueue")
    public ResponseEntity<Void>  uploadFileToQueue(@RequestParam(value = "queueName", required = true) String queueName,
                                            @RequestParam(value = "file", required = true) MultipartFile file,
                                            @RequestParam(value = "mode", required = false, defaultValue = "text") String mode,
                                            @RequestParam(value = "len", required = false, defaultValue = "0") int len,
                                            @RequestHeader HttpHeaders headers) {
        fileDumpingService.dumpFile(queueName, Objects.requireNonNull(headers.get(HttpHeaders.CONTENT_TYPE)), mode, len, file);
        return new ResponseEntity<>(HttpStatus.OK);
    }
    @PostMapping("/rabbit/uploadToExchange")
    public ResponseEntity<Void>  uploadFileToExchange(@RequestParam(value = "exchangeName", required = true) String exchangeName,
                                            @RequestParam(value = "routingKey", required = true) String routingKey,
                                            @RequestParam(value = "file", required = true) MultipartFile file,
                                            @RequestParam(value = "mode", required = false, defaultValue = "text") String mode,
                                            @RequestParam(value = "len", required = false, defaultValue = "0") int len,
                                            @RequestHeader HttpHeaders headers) {
        fileDumpingService.dumpFile(exchangeName, routingKey, Objects.requireNonNull(headers.get(HttpHeaders.CONTENT_TYPE)), mode, len, file);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
