package com.danir.libraryAPI.controllers;

import com.danir.libraryAPI.models.Book;
import com.danir.libraryAPI.models.Person;
import com.danir.libraryAPI.services.BookService;
import com.danir.libraryAPI.services.PaymentService;
import com.danir.libraryAPI.services.PeopleService;
import com.danir.libraryAPI.util.PersonDetails;
import com.danir.libraryAPI.util.StripeConfig;
import com.stripe.exception.StripeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/payment")
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final BookService bookService;
    private final PeopleService peopleService;
    private final StripeConfig stripeConfig;

    public PaymentController(PaymentService paymentService, BookService bookService, PeopleService peopleService, StripeConfig stripeConfig) {
        this.paymentService = paymentService;
        this.bookService = bookService;
        this.peopleService = peopleService;
        this.stripeConfig = stripeConfig;
    }

    @GetMapping("/{bookId}")
    public String initiatePayment(@PathVariable("bookId") int bookId, Model model) {
        log.info("Initiating payment for book ID: {}", bookId);

        try {
            Book book = bookService.findOne(bookId);
            if (book.getIsDebtPaid()) {
                log.info("Book ID: {} has no debt to pay.", bookId);
                model.addAttribute("message", "This book has no debt to pay.");
                return "book/bookShow";
            }

            String clientSecret = paymentService.createPaymentIntentForBook(book);
            addPaymentAttributes(model, clientSecret, bookService.calculateDebt(book));

            log.info("Payment initiated successfully for book ID: {}", bookId);
            return "payment/payment";
        } catch (StripeException e) {
            log.error("Stripe payment failed for book ID: {}", bookId, e);
            model.addAttribute("error", "Payment failed: " + e.getMessage());
            return "payment/payment-error";
        }
    }

    @GetMapping("/all/{id}")
    public String initiatePaymentForAllBooks(@PathVariable("id") int personId, Model model) {
        log.info("Initiating payment for all books of person ID: {}", personId);

        Person person = peopleService.findOne(personId);
        try {
            List<Book> booksWithDebt = person.getBookList().stream().filter(book -> !book.getIsDebtPaid()).toList();
            if (booksWithDebt.isEmpty()) {
                log.info("No books with debt found for person ID: {}", personId);
                model.addAttribute("message", "You don't have any books with debt to pay.");
                return "book/bookShow";
            }

            String clientSecret = paymentService.createPaymentIntentForAllBooks(booksWithDebt);
            addPaymentAttributes(model, clientSecret, bookService.calculateTotalDebt(person));
            model.addAttribute("person", person);

            log.info("Payment initiated successfully for all books of person ID: {}", personId);
            return "payment/payment-multiple";
        } catch (StripeException e) {
            log.error("Stripe payment failed for all books of person ID: {}", personId, e);
            model.addAttribute("error", "Payment failed: " + e.getMessage());
            return "payment/payment-error";
        }
    }

    @PostMapping("/confirm")
    public String confirmPayment(@RequestParam String paymentIntentId, @RequestParam int bookId, RedirectAttributes redirectAttributes) {
        log.info("Confirming payment for book ID: {}", bookId);

        Person person = getCurrentPerson();
        Book book = bookService.findOne(bookId);
        try {
            paymentService.confirmPayment(paymentIntentId, person, book);
            log.info("Payment confirmed successfully for book ID: {}", bookId);
            redirectAttributes.addFlashAttribute("message", "Payment successful!");
            return "payment/payment-success";
        } catch (StripeException e) {
            log.error("Payment confirmation failed for book ID: {}", bookId, e);
            redirectAttributes.addFlashAttribute("error", "Payment verification failed: " + e.getMessage());
            return "payment/payment-error";
        }
    }

    @PostMapping("/confirm-multiple/{id}")
    public String confirmMultiplePayments(@PathVariable("id") int personId, @RequestParam String paymentIntentId, RedirectAttributes redirectAttributes) {
        log.info("Confirming multiple payments for person ID: {}", personId);

        Person person = peopleService.findOne(personId);
        try {
            paymentService.confirmPaymentForAllBooks(paymentIntentId, person);
            log.info("Multiple payments confirmed successfully for person ID: {}", personId);
            redirectAttributes.addFlashAttribute("message", "Payment successful!");
            return "payment/payment-success";
        } catch (StripeException e) {
            log.error("Multiple payments confirmation failed for person ID: {}", personId, e);
            redirectAttributes.addFlashAttribute("error", "Payment verification failed: " + e.getMessage());
            return "payment/payment-error";
        }
    }

    @GetMapping("/payment-success")
    public String paymentSuccess() {
        log.info("Accessed payment success page.");
        return "payment/payment-success";
    }

    private Person getCurrentPerson() {
        PersonDetails personDetails = (PersonDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return personDetails.getPerson();
    }

    private void addPaymentAttributes(Model model, String clientSecret, double amount) {
        model.addAttribute("publishableKey", stripeConfig.getPublishableKey());
        model.addAttribute("clientSecret", clientSecret);
        model.addAttribute("amount", amount);
        model.addAttribute("currency", "EUR");
    }
}