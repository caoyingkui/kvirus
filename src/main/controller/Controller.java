package main.controller;

/**
 * Created by oliver on 2018/11/5.
 */
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.ArrayList;
import java.util.List;


@EnableScheduling
@Controller
public class WebController {
    List<String> messageList = new ArrayList<>();
    int messageStart = 0;


    int id = 0;

    @Autowired
    private SimpMessagingTemplate template;

    @MessageMapping("/search")
    public void codeSearch(SearchRequest request) throws Exception{
        String path = "";
        git.GitAnalyzer analyzer = new git.GitAnalyzer(path);


    }

    public void feedBackMessage(String feedback){
        this.template.convertAndSend("/message/feedback", new FeedbackMessage(0, feedback));
    }

    public void returnResultMessage(){
        this.template.convertAndSend("/message/result", searchMessage);
    }
}