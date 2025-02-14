package com.danir.libraryAPI.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.OffsetDateTime;

@Entity
@Table(name = "payment")
@Data
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id", nullable = false)
    private int paymentId;

    @ManyToOne
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Column
    private String bookTitle;

    @Column(name = "amount", nullable = false)
    private double amount;

    @Column(name = "payment_date", nullable = false)
    private OffsetDateTime paymentDate;

    public Payment() {}

    public Payment(Person person, String bookTitle, double amount, OffsetDateTime paymentDate) {
        this.person = person;
        this.bookTitle = bookTitle;
        this.amount = amount;
        this.paymentDate = paymentDate;
    }
}
