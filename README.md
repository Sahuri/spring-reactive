# Implementasi Event Listener di Spring Boot (Asynchronous Event-Driven)

Dokumentasi ini menjelaskan cara kerja, pembuatan Controller, dan Service untuk memproses data transaksi menggunakan mekanisme **ApplicationEventPublisher** dan **@EventListener** di Spring Boot.

---

## 1. Apa itu Event Listener?

**Event Listener** adalah pola desain (*design pattern*) di mana sebuah komponen (Listener) menunggu dan merespons sebuah kejadian (*event*) yang dipicu oleh komponen lain (Publisher).

Di Spring Boot, mekanisme ini memisahkan proses penerimaan data dari proses pengolahan data (*decoupling*). Keuntungan utamanya meliputi:
*   **Non-blocking**: Controller bisa langsung mengembalikan respons `202 Accepted` tanpa menunggu Service selesai memproses data.
*   **Maintanable**: Kode menjadi lebih bersih karena logika bisnis terpisah dari logika HTTP Controller.

---

## 2. Controller: Menerima Request dan Memicu Event

Controller di bawah ini berfungsi sebagai **Publisher**. Saat menerima data transaksi via HTTP POST, Controller akan membungkus data tersebut ke dalam objek `DataMapper` dan memublikasikannya sebagai sebuah *event*.

```java
@RestController
@RequestMapping("/api/v1/transaction")
public class TransactionController {

    @Autowired
    private ApplicationEventPublisher publisher;

    @PostMapping(path = "/submit")
    public Mono<ResponseEntity<ResponseResult<String>>> createTransaction(@RequestBody List<TransactionDto> transactionDtoList) {
        // Memicu event dan mengirim data transaksi ke listener
        publisher.publishEvent(new DataMapper<>(transactionDtoList));
        
        // Langsung mengembalikan respons tanpa menunggu proses di service selesai
        return Mono.just(ResponseEntity.accepted().body(
            new ResponseResult<>(HttpStatus.ACCEPTED.toString(), HttpStatus.ACCEPTED.name(), "Submitting data successful")
        ));
    }
}
```

---

## 3. Service: Mengonsumsi dan Memproses Data

Service di bawah ini bertindak sebagai **Listener**. Dengan anotasi `@EventListener`, metode `submit` akan otomatis berjalan setiap kali ada *event* baru dengan tipe data `DataMapper<List<TransactionDto>>`.

```java
@Service
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    @EventListener
    public void submit(DataMapper<List<TransactionDto>> dataMapper) {
        // Mengambil payload data transaksi dari event
        List<TransactionDto> transactionDtoList = dataMapper.getPayload();
        AtomicInteger i = new AtomicInteger(1);
        
        // Memproses/mencetak setiap transaksi ke log
        transactionDtoList.stream().forEach(trx -> {
            logger.info("Trx {}: {}", i.getAndIncrement(), trx);
        });
    }
}
```
---

## 4. Struktur DataMapper (Event Wrapper)

Untuk mengirimkan data dari Controller ke Service, kita menggunakan sebuah kelas *wrapper* generic bernama `DataMapper`. Kelas ini berfungsi membawa *payload* data yang akan ditransmisikan melalui event.

```java
public class DataMapper<T> {
    private final T payload;

    public DataMapper(T payload) {
        this.payload = payload;
    }

    public T getPayload() {
        return payload;
    }
}
```

---

## 5. Mengaktifkan Proses Asynchronous (@Async)

Secara bawaan (*default*), mekanisme `ApplicationEventPublisher` di Spring Boot berjalan secara sinkron (*synchronous*). Artinya, Controller tetap akan menunggu proses di Service selesai sebelum mengirim respons HTTP.

Agar proses berjalan secara **Asynchronous (Non-blocking)** sepenuhnya, kita perlu melakukan dua langkah berikut:

### Langkah 1: Aktifkan Async di Kelas Konfigurasi
Tambahkan anotasi `@EnableAsync` pada kelas utama (*Main Class*) atau kelas konfigurasi Spring Boot Anda.

```java
@SpringBootApplication
@EnableAsync // <--- Mengaktifkan fitur asynchronous
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### Langkah 2: Tambahkan @Async pada Event Listener
Tambahkan anotasi `@Async` tepat di atas anotasi `@EventListener` pada komponen Service Anda.

```java
@Service
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    @Async // <--- Membuat metode ini berjalan di thread terpisah
    @EventListener
    public void submit(DataMapper<List<TransactionDto>> dataMapper) {
        List<TransactionDto> transactionDtoList = dataMapper.getPayload();
        AtomicInteger i = new AtomicInteger(1);
        
        transactionDtoList.stream().forEach(trx -> {
            logger.info("Trx {}: {}", i.getAndIncrement(), trx);
        });
    }
}
```

Dengan konfigurasi di atas, saat `publisher.publishEvent()` dipanggil, Spring akan langsung mengeksekusi metode `submit` di *thread* yang berbeda. Controller bisa langsung mengembalikan respons `202 Accepted` ke pengguna tanpa jeda.

