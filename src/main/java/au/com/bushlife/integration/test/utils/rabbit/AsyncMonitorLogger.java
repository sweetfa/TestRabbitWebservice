package au.com.bushlife.integration.test.utils.rabbit;

import static java.text.MessageFormat.format;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;

@Slf4j
@RequiredArgsConstructor
public class AsyncMonitorLogger {

  private final RabbitTemplate template;

  @Async
  public void monitor(String queueName)  {

    do {
      Message response = template.receive(queueName, 60000);
      if (response != null) {
        log.info("Received response");
        var responseStr = new String(response.getBody());
        log.info(format("Received [{0}]", responseStr));
      }
    } while (true);
  }
}
