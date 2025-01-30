package com.danir.libraryAPI.controllers;

import com.danir.libraryAPI.models.Book;
import com.danir.libraryAPI.models.Person;
import com.danir.libraryAPI.services.BookService;
import com.danir.libraryAPI.services.PaymentService;
import com.danir.libraryAPI.services.PeopleService;
import com.danir.libraryAPI.util.PersonDetails;
import com.danir.libraryAPI.util.StripeConfig;
import com.stripe.exception.StripeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/payment")
public class PaymentController {

    private final PaymentService paymentService;
    private final BookService bookService;
    private final PeopleService peopleService;
    private final StripeConfig stripeConfig;
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    public PaymentController(PaymentService paymentService, BookService bookService, PeopleService peopleService, StripeConfig stripeConfig) {
        this.paymentService = paymentService;
        this.bookService = bookService;
        this.peopleService = peopleService;
        this.stripeConfig = stripeConfig;
    }

    @GetMapping("/{bookId}")
    public String initiatePayment(@PathVariable("bookId") int bookId, Model model) {
        try {
            Book book = bookService.findOne(bookId);
            if (book.getIsDebtPaid()) {
                model.addAttribute("message", "This book has no debt to pay.");
                return "book/bookShow";
            }
            String clientSecret = paymentService.createPaymentIntentForBook(book);
            addPaymentAttributes(model, clientSecret, bookService.calculateDebt(book));
            return "payment/payment";
        } catch (StripeException e) {
            logger.error("Stripe payment failed for bookId: " + bookId, e);
            model.addAttribute("error", "Payment failed: " + e.getMessage());
            return "payment/payment-error";
        }
    }

    @GetMapping("/all/{id}")
    public String initiatePaymentForAllBooks(@PathVariable("id") int personId, Model model) {
        Person person = peopleService.findOne(personId);
        try {
            System.out.println("зашли в алл букс");
            List<Book> booksWithDebt = person.getBookList().stream().filter(book -> !book.getIsDebtPaid()).toList();
            if (booksWithDebt.isEmpty()) {
                model.addAttribute("message", "You don't have any books with debt to pay.");
                return "book/bookShow";
            }
            String clientSecret = paymentService.createPaymentIntentForAllBooks(booksWithDebt);
            addPaymentAttributes(model, clientSecret, bookService.calculateTotalDebt(person));
            model.addAttribute("person", person);
            System.out.println("сделали алл букс");
            return "payment/payment-multiple";
        } catch (StripeException e) {
            logger.error("Stripe payment failed for all books", e);
            model.addAttribute("error", "Payment failed: " + e.getMessage());
            return "payment/payment-error";
        }
    }

    @PostMapping("/confirm")
    public String confirmPayment(@RequestParam String paymentIntentId, @RequestParam int bookId, RedirectAttributes redirectAttributes) {
        Person person = getCurrentPerson();
        Book book = bookService.findOne(bookId);
        try {
            paymentService.confirmPayment(paymentIntentId, person, book);
            redirectAttributes.addFlashAttribute("message", "Payment successful!");
            return "payment/payment-success";
        } catch (StripeException e) {
            redirectAttributes.addFlashAttribute("error", "Payment verification failed: " + e.getMessage());
            return "payment/payment-error";
        }
    }

    @PostMapping("/confirm-multiple/{id}")
    public String confirmMultiplePayments(@PathVariable("id") int personId, @RequestParam String paymentIntentId, RedirectAttributes redirectAttributes) {
        Person person = peopleService.findOne(personId);
        try {
            paymentService.confirmPaymentForAllBooks(paymentIntentId, person);
            redirectAttributes.addFlashAttribute("message", "Payment successful!");
            return "payment/payment-success";
        } catch (StripeException e) {
            redirectAttributes.addFlashAttribute("error", "Payment verification failed: " + e.getMessage());
            return "payment/payment-error";
        }
    }

    @GetMapping("/payment-success")
    public String paymentSuccess() {
        System.out.println("зашли в саксес");
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
