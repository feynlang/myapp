package com.example.scp.question;

import java.time.LocalDateTime;
import java.util.List;

import com.example.scp.answer.Answer;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Question {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Integer id;

    @Column(length=200)
    private String subject;
    
    @Column(columnDefinition="TEXT")
    private String content;

    private LocalDateTime createDate;

    @OneToMany(mappedBy="question", fetch=FetchType.EAGER, cascade=CascadeType.REMOVE)
    private List<Answer> answerList;
}
