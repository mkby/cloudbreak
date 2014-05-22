package com.sequenceiq.provisioning.controller;

import java.util.Set;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sequenceiq.provisioning.controller.json.TemplateJson;
import com.sequenceiq.provisioning.domain.User;
import com.sequenceiq.provisioning.repository.UserRepository;
import com.sequenceiq.provisioning.security.CurrentUser;
import com.sequenceiq.provisioning.service.TemplateService;

@Controller
@RequestMapping("template")
public class TemplateController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TemplateService templateService;

    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<String> createTemplate(@CurrentUser User user, @RequestBody @Valid TemplateJson templateRequest) {
        templateService.create(userRepository.findOneWithLists(user.getId()), templateRequest);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<TemplateJson>> getAllTemplates(@CurrentUser User user) {
        return new ResponseEntity<>(templateService.getAll(userRepository.findOneWithLists(user.getId())), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, value = "{templateId}")
    @ResponseBody
    public ResponseEntity<TemplateJson> getTemplate(@CurrentUser User user, @PathVariable Long templateId) {
        TemplateJson templateRequest = templateService.get(templateId);
        return new ResponseEntity<>(templateRequest, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "{templateId}")
    @ResponseBody
    public ResponseEntity<TemplateJson> deleteTemplate(@CurrentUser User user, @PathVariable Long templateId) {
        templateService.delete(templateId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
