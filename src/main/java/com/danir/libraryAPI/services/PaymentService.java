package com.danir.libraryAPI.services;

import com.danir.libraryAPI.models.Book;
import com.danir.libraryAPI.models.Payment;
import com.danir.libraryAPI.models.Person;
import com.danir.libraryAPI.repositories.PaymentRepository;
import com.danir.libraryAPI.util.StripeConfig;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    private final PaymentRepository paymentRepository;
    private final BookService bookService;
    private final StripeConfig stripeConfig;

    public PaymentService(PaymentRepository paymentRepository, BookService bookService, StripeConfig stripeConfig) {
        this.paymentRepository = paymentRepository;
        this.bookService = bookService;
        this.stripeConfig = stripeConfig;
    }

    @Transactional(rollbackFor = StripeException.class)
    public String createPaymentIntentForBook(Book book) throws StripeException {
        logger.info("Creating payment intent for book: {}", book.getName());
        validateBook(book);

        long daysHeld = ChronoUnit.DAYS.between(book.getBorrowedDate(), OffsetDateTime.now());
        double amount = calculateAmount(daysHeld);

        logger.debug("Calculated amount for book '{}': {}", book.getName(), amount);

        // Create payment intent in stripe
        String clientSecret = createStripePaymentIntent(amount, "Payment for book: " + book.getName());

        logger.info("Payment intent created successfully for book: {}", book.getName());
        return clientSecret;
    }

    @Transactional(rollbackFor = StripeException.class)
    public String createPaymentIntentForAllBooks(List<Book> books) throws StripeException {
        logger.info("Creating payment intent for all books");
        double totalAmount = books.stream()
                .filter(book -> !book.getIsDebtPaid())
                .mapToDouble(book -> calculateAmount(ChronoUnit.DAYS.between(book.getBorrowedDate(), OffsetDateTime.now())))
                .sum();

        if (totalAmount == 0) {
            logger.warn("No unpaid books found to create payment intent.");
            throw new IllegalStateException("No unpaid books to pay for.");
        }

        logger.debug("Total amount calculated for all books: {}", totalAmount);
        return createStripePaymentIntent(totalAmount, "Payment for multiple books");
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirmPayment(String paymentIntentId, Person person, Book book) throws StripeException {
        logger.info("Confirming payment for book: {}", book.getName());
        verifyPayment(paymentIntentId);

        long daysHeld = ChronoUnit.DAYS.between(book.getBorrowedDate(), OffsetDateTime.now());
        double amount = calculateAmount(daysHeld);

        Payment payment = new Payment(person, book.getName(), amount, OffsetDateTime.now());
        paymentRepository.save(payment);

        logger.debug("Payment saved for book '{}' with amount: {}", book.getName(), amount);
        markBookAsPaid(book);

        logger.info("Payment confirmed and book marked as paid: {}", book.getName());
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirmPaymentForAllBooks(String paymentIntentId, Person person) throws StripeException {
        logger.info("Confirming payment for all books of person: {}", person.getFullName());
        verifyPayment(paymentIntentId);

        OffsetDateTime now = OffsetDateTime.now();
        List<Book> books = person.getBookList().stream()
                .filter(book -> !book.getIsDebtPaid()).toList();

        double totalAmount = books.stream()
                .mapToDouble(book -> calculateAmount(ChronoUnit.DAYS.between(book.getBorrowedDate(), now)))
                .sum();

        if (totalAmount == 0) {
            logger.warn("No unpaid books found for person: {}", person.getFullName());
            throw new IllegalStateException("No unpaid books to confirm payment for.");
        }

        StringBuilder stringBuilder = new StringBuilder();
        for (Book book : books) {
            stringBuilder.append(book.getName()).append(",");
        }
        if (!stringBuilder.isEmpty()) {
            stringBuilder.deleteCharAt(stringBuilder.length() - 1); // delete last ','
        }

        Payment payment = new Payment(person, stringBuilder.toString(), totalAmount, now);
        paymentRepository.save(payment);

        logger.debug("Payment saved for books: {} with total amount: {}", stringBuilder, totalAmount);

        for (Book book : books) {
            markBookAsPaid(book);
        }

        logger.info("Payment confirmed and all books marked as paid for person: {}", person.getFullName());
    }

    private String createStripePaymentIntent(double amount, String description) throws StripeException {
        logger.debug("Creating Stripe payment intent with amount: {} and description: {}", amount, description);
        Stripe.apiKey = stripeConfig.getSecretKey();

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount((long) (amount * 100)) // В центах
                .setCurrency(stripeConfig.getCurrency())
                .setDescription(description)
                .build();

        return PaymentIntent.create(params).getClientSecret();
    }

    private void verifyPayment(String paymentIntentId) throws StripeException {
        logger.debug("Verifying payment with ID: {}", paymentIntentId);
        PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
        if (!"succeeded".equals(paymentIntent.getStatus())) {
            logger.error("Payment not successful for payment intent ID: {}", paymentIntentId);
//            throw new StripeException("Payment not successful.");
        }
    }

    private void validateBook(Book book) {
        if (book.getBorrowedDate() == null) {
            logger.error("Borrowed date is not set for book: {}", book.getName());
            throw new IllegalArgumentException("Borrowed date is not set for the book.");
        }
        if (book.getIsDebtPaid()) {
            logger.warn("Book '{}' is already paid.", book.getName());
            throw new IllegalStateException("The book is already paid.");
        }
    }

    private void markBookAsPaid(Book book) {
        logger.debug("Marking book '{}' as paid.", book.getName());
        book.setDebt(0.0);
        book.setIsDebtPaid(true);
        book.setPaymentDate(OffsetDateTime.now());
        bookService.save(book);
        logger.info("Book '{}' marked as paid successfully.", book.getName());
    }

    private double calculateAmount(long daysHeld) {
        double amount = (daysHeld <= 10) ? daysHeld * 1.0 : 10 + (daysHeld - 10) * 5.0;
        logger.debug("Calculated amount for {} days: {}", daysHeld, amount);
        return amount;
    }

    @Transactional
    @Scheduled(cron = "0 0 0 * * *")
    public void checkIfBookReturnedAfterPayment() {
        logger.info("Running scheduled task to check if books are returned after payment.");
        List<Book> allBooks = bookService.findAll()
                .stream()
                .filter(book -> book.getPerson() != null)
                .toList();

        for (Book book : allBooks) {
            long hoursSincePayment = ChronoUnit.HOURS.between(book.getPaymentDate().toInstant(), Instant.now());

            if (hoursSincePayment > 24 && book.getPaymentDate() != null) {
                logger.warn("Book '{}' was not returned within 24 hours after payment.", book.getName());
                book.setIsDebtPaid(false);
                bookService.save(book);
            }
        }
        logger.info("Scheduled task completed.");
    }
}