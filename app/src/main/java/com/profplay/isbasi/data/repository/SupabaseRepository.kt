package com.profplay.isbasi.data.repository

import android.util.Log
import com.profplay.isbasi.data.model.Job
import com.profplay.isbasi.data.model.User
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.result.PostgrestResult
import kotlinx.serialization.Serializable
import kotlin.coroutines.cancellation.CancellationException
import com.profplay.isbasi.data.model.Application
import com.profplay.isbasi.data.model.Review
import com.profplay.isbasi.data.model.ReviewWithReviewer
import io.github.jan.supabase.postgrest.query.Columns


class SupabaseRepository (
    private val supabase: SupabaseClient
) {
    /*todo: yorum satırları ve debuglar kalkacak */
    /*todo: getHumans tekrar aktif hale gelecek*/
    /* // --- BU FONKSİYONU GEÇİCİ OLARAK YORUMA AL ---
    suspend fun getHumans(): List<User> {
        Log.d("RepoDebug", "getHumans: Fonksiyon BAŞLADI. Supabase client null mu? ${supabase == null}")
        if (supabase == null) {
            Log.e("RepoDebug", "getHumans: Supabase client NULL, boş liste dönülüyor.")
            return emptyList()
        }

        try { // Supabase çağrısını try içine alalım (zaten olabilir)
            Log.d("RepoDebug", "getHumans: Supabase'e select sorgusu gönderiliyor...") // <-- YENİ LOG
            val response: PostgrestResult = supabase
                .from("users")
                .select()
            Log.d("RepoDebug", "getHumans: Supabase'den yanıt alındı.") // <-- YENİ LOG
            val list = response.decodeList<User>()
            list.forEach { human ->
                Log.d("SupabaseDebug", "Fetched human id: ${human.id}")
            }
            return list
        } catch (e: Exception) {
            Log.e("RepoDebug", "getHumans CATCH'E DÜŞTÜ! ASIL HATA: ${e.message}", e)
            return emptyList() // Hata durumunda boş liste dön
        }
    }
    */ // --------------------------------------------
    // SupabaseRepository.kt içinde
    suspend fun getHuman(): User? {
        Log.d("RepoDebug", "getHuman: Fonksiyon BAŞLADI. Supabase client null mu? ${supabase == null}")
        if (supabase == null) {
            Log.e("RepoDebug", "getHuman: Supabase client NULL, null dönülüyor.")
            return null
        }

        // --- userId'yi BURADA TANIMLA VE KONTROL ET ---
        val userId = supabase.auth.currentUserOrNull()?.id
        if (userId == null) {
            Log.w("RepoDebug", "getHuman: Kullanıcı ID'si alınamadı (currentUserOrNull null döndü). Giriş yapılmamış olabilir.")
            return null // ID yoksa sorgu atmaya gerek yok, null dön
        }
        // ---------------------------------------------

        Log.d("RepoDebug", "getHuman: userId: $userId alındı.")

        try {
            Log.d("RepoDebug", "getHuman: Supabase'e select sorgusu gönderiliyor...")
            val response: PostgrestResult = supabase
                .from("users")
                // Şimdi 'userId' tanımlı olduğu için bu satır çalışır
                .select { filter { eq("id", userId) } }
            Log.d("RepoDebug", "getHuman: Supabase'den yanıt alındı.")

            // Yanıtın gövdesini (body) de loglayalım, belki hata mesajı oradadır
            Log.d("RepoDebug", "getHuman: Yanıt body: ${response.data}")

            val list = response.decodeList<User>()
            if (list.isEmpty()) {
                // userId'yi burada da kullanabiliriz
                Log.w("RepoDebug", "getHuman: Sorgu başarılı ama BOŞ liste döndü (ID: $userId için kayıt yok veya RLS engelledi?).")
                return null
            }
            Log.d("RepoDebug", "getHuman: Kayıt bulundu, decode edildi: ${list.firstOrNull()}")
            return list.firstOrNull()
        } catch (e: CancellationException) {
            Log.w("RepoDebug", "getHuman İPTAL EDİLDİ (CancellationException).")
            throw e
        } catch (e: Exception) {
            Log.e("RepoDebug", "getHuman CATCH'E DÜŞTÜ! ASIL HATA: ${e.message}", e)
            return null
        }
    }

    suspend fun signUp(email: String, password: String, role: String): String? {
        val signUpResult = supabase.auth.signUpWith(
            provider = Email,
            config = {
                this.email = email
                this.password = password
            }
        )
        val userId = signUpResult?.id
        if (userId != null) {
            supabase.postgrest["users"].insert(
                mapOf(
                    "id" to userId,
                    "role" to role
                )
            )
        }
        return userId
    }

    suspend fun signIn(email: String, password: String): String? {
        return try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            // Başarılı giriş sonrası kullanıcının ID'sini döndür
            supabase.auth.currentUserOrNull()?.id
        } catch (e: Exception) {
            // Hata durumunda null döndür
            null
        }
    }

    suspend fun signOut() {
        supabase.auth.signOut()
    }

    fun currentUserId(): String? {
        return supabase.auth.currentUserOrNull()?.id
    }
    fun isLoggedIn(): Boolean {
        return supabase.auth.currentSessionOrNull() != null
    }

    // Bu fonksiyonu SupabaseRepository sınıfına ekleyin
    suspend fun getUserRole(userId: String): String {
        return try {
            // "users" tablosundan ilgili kullanıcının verisini çek
            val userProfile = supabase.from("users")
                .select { filter { eq("id", userId) } }
                .decodeSingle<UserProfile>() // UserProfile bir data class olmalı

            // Dönen veriden "role" alanını al
            userProfile.role
        } catch (e: Exception) {
            // Hata veya kullanıcı profili bulunamazsa varsayılan bir rol döndür
            Log.e("GET_ROLE", "Rol alınamadı: ${e.message}")
            "employee" // Varsayılan olarak "işçi" rolü
        }
    }

    // Repository'nin kullandığı bir data class oluşturman lazım
    // Örnek: com.profplay.isbasi.data.model altında UserProfile.kt
    @Serializable
    data class UserProfile(
        val id: String,
        val name: String? = null,
        val role: String,
        val rating: Float? = null,
        val bio: String? = null
    )

    /**
     * Kullanıcının profil bilgilerini (ad, biyografi vb.) günceller.
     * @param userId Güncellenecek kullanıcının ID'si.
     * @param name Kullanıcının yeni adı.
     * @param bio Kullanıcının yeni biyografisi.
     * @return Güncelleme başarılıysa true, değilse false döner.
     */
    suspend fun updateProfile(userId: String, name: String?, bio: String?): Boolean {
        return try {
            // "users" tablosuna git
            supabase.from("users")
                .update(
                    // Güncellenecek sütunları bir harita (map) olarak ver
                    mapOf(
                        "name" to name,
                        "bio" to bio
                        // İleride buraya "profile_image_url" gibi başka alanlar da ekleyebilirsiniz.
                    )
                ) {
                    // Hangi satırın güncelleneceğini filtrele
                    filter {
                        eq("id", userId)
                    }
                }

            // Buraya kadar geldiyse (hata fırlatmadıysa) başarılıdır.
            true
        } catch (e: Exception) {
            Log.e("SupabaseRepository", "Profil güncellenirken hata oluştu: ${e.message}")
            false
        }
    }
    /**
     * Yeni bir iş ilanı oluşturur.
     * @param title İlan başlığı
     * @param description İlan açıklaması
     * @param location İşin konumu
     * @param dateStart İşin başlama tarihi (örn: "2025-10-20")
     * @param minRating İstenen minimum işçi puanı (opsiyonel)
     * @return Oluşturma başarılıysa true, değilse false döner.
     */
    suspend fun createJob(
        title: String,
        description: String,
        location: String,
        dateStart: String,
        dateEnd: String,
        minRating: Float?
    ): Boolean {
        return try {
            // Mevcut giriş yapmış kullanıcının ID'sini al
            val currentEmployerId = supabase.auth.currentUserOrNull()?.id
            if (currentEmployerId == null) {
                Log.e("SupabaseRepository", "createJob: Kullanıcı giriş yapmamış.")
                return false
            }

            // Yeni Job nesnesini oluştur
            val newJob = Job(
                employerId = currentEmployerId, // RLS kuralının beklediği ID
                title = title,
                description = description,
                location = location,
                dateStart = dateStart,
                dateEnd = dateEnd,
                minRating = minRating
                // id ve createdAt otomatik atanacak
            )

            // "jobs" tablosuna yeni oluşturduğumuz nesneyi ekle
            supabase.from("jobs").insert(newJob)

            // Buraya kadar geldiyse (hata fırlatmadıysa) başarılıdır.
            true
        } catch (e: Exception) {
            Log.e("SupabaseRepository", "İş ilanı oluşturulurken hata: ${e.message}")
            false
        }
    }

    /**
     * Tüm iş ilanlarını, başlangıç tarihine göre sıralı olarak getirir.
     * @return İş ilanlarının listesi veya hata durumunda boş liste.
     */
    suspend fun getAllJobs(): List<Job> {
        Log.d("RepoDebug", "getAllJobs: Fonksiyon BAŞLADI.")
        if (supabase == null) {
            Log.e("RepoDebug", "getAllJobs: Supabase client NULL.")
            return emptyList()
        }
        try {
            Log.d("RepoDebug", "getAllJobs: Supabase'e select sorgusu gönderiliyor...")
            val response: PostgrestResult = supabase
                .from("jobs")
                .select {
                    // Tarihe göre sırala (eskiden yeniye)
                    order("date_start", Order.ASCENDING)
                }
            Log.d("RepoDebug", "getAllJobs: Supabase'den yanıt alındı. Data: ${response.data}")
            val list = response.decodeList<Job>()
            Log.i("RepoDebug", "getAllJobs: Başarıyla ${list.size} iş çekildi.")
            return list
        } catch (e: CancellationException) {
            Log.w("RepoDebug", "getAllJobs İPTAL EDİLDİ.")
            throw e // İptali yukarı fırlat
        } catch (e: Exception) {
            Log.e("RepoDebug", "getAllJobs CATCH'E DÜŞTÜ! ASIL HATA: ${e.message}", e)
            return emptyList() // Hata durumunda boş liste dön
        }
    }

    /**
     * Belirli bir işverene ait iş ilanlarını, başlangıç tarihine göre sıralı olarak getirir.
     * @param employerId İşverenin kullanıcı ID'si.
     * @return İş ilanlarının listesi veya hata durumunda boş liste.
     */
    suspend fun getJobsByEmployerId(employerId: String): List<Job> {
        Log.d("RepoDebug", "getJobsByEmployerId: Fonksiyon BAŞLADI (ID: $employerId).")
        if (supabase == null) {
            Log.e("RepoDebug", "getJobsByEmployerId: Supabase client NULL.")
            return emptyList()
        }
        try {
            Log.d("RepoDebug", "getJobsByEmployerId: Supabase'e select sorgusu gönderiliyor...")
            val response: PostgrestResult = supabase
                .from("jobs")
                .select {
                    // Sadece belirtilen işverene ait olanları filtrele
                    filter {
                        eq("employer_id", employerId)
                    }
                    // Tarihe göre sırala
                    order("date_start", Order.ASCENDING)
                }
            Log.d("RepoDebug", "getJobsByEmployerId: Supabase'den yanıt alındı. Data: ${response.data}")
            val list = response.decodeList<Job>()
            Log.i("RepoDebug", "getJobsByEmployerId: Başarıyla ${list.size} iş çekildi.")
            return list
        } catch (e: CancellationException) {
            Log.w("RepoDebug", "getJobsByEmployerId İPTAL EDİLDİ.")
            throw e
        } catch (e: Exception) {
            Log.e("RepoDebug", "getJobsByEmployerId CATCH'E DÜŞTÜ! ASIL HATA: ${e.message}", e)
            return emptyList()
        }
    }

    /**
     * Belirli bir tarih aralığındaki TÜM iş ilanlarını, başlangıç tarihine göre sıralı olarak getirir.
     * Supabase 'date' tipi 'yyyy-MM-dd' formatını bekler.
     * @param startDate Başlangıç tarihi (örn: "2025-10-20").
     * @param endDate Bitiş tarihi (örn: "2025-10-27").
     * @return İş ilanlarının listesi veya hata durumunda boş liste.
     */
    suspend fun getJobsByDateRange(startDate: String, endDate: String): List<Job> {
        Log.d("RepoDebug", "getJobsByDateRange: Fonksiyon BAŞLADI ($startDate - $endDate).")
        if (supabase == null) {
            Log.e("RepoDebug", "getJobsByDateRange: Supabase client NULL.")
            return emptyList()
        }
        try {
            Log.d("RepoDebug", "getJobsByDateRange: Supabase'e select sorgusu gönderiliyor...")
            val response: PostgrestResult = supabase
                .from("jobs")
                .select {
                    // Tarih aralığı filtresi: date_start >= startDate AND date_start <= endDate
                    filter {
                        gte("date_start", startDate) // >= Başlangıç tarihi
                        lte("date_start", endDate)   // <= Bitiş tarihi
                    }
                    // Tarihe göre sırala
                    order("date_start", Order.ASCENDING)
                }
            Log.d("RepoDebug", "getJobsByDateRange: Supabase'den yanıt alındı. Data: ${response.data}")
            val list = response.decodeList<Job>()
            Log.i("RepoDebug", "getJobsByDateRange: Başarıyla ${list.size} iş çekildi.")
            return list
        } catch (e: CancellationException) {
            Log.w("RepoDebug", "getJobsByDateRange İPTAL EDİLDİ.")
            throw e
        } catch (e: Exception) {
            Log.e("RepoDebug", "getJobsByDateRange CATCH'E DÜŞTÜ! ASIL HATA: ${e.message}", e)
            return emptyList()
        }
    }

    /**
     * İşçinin bir iş ilanına başvurmasını sağlar.
     * @param jobId Başvurulan işin ID'si
     * @return Başvuru başarılıysa true, değilse false.
     */
    suspend fun applyForJob(jobId: String, workerId: String): Boolean {
        return try {
            Log.d("RepoDebug", "applyForJob: Başvuru gönderiliyor... JobID: $jobId, WorkerID: $workerId")
            supabase.from("applications").insert(
                mapOf(
                    "job_id" to jobId,
                    "worker_id" to workerId, // Parametreden gelen ID'yi kullan
                    "status" to "pending"
                )
            )
            Log.i("RepoDebug", "applyForJob: Başvuru BAŞARILI.")
            true
        } catch (e: Exception) {
            Log.e("RepoDebug", "applyForJob HATA: ${e.message}", e)
            false
        }
    }

    /**
     * Belirli bir işçinin TÜM başvurularını getirir.
     * @param workerId İşçinin ID'si.
     * @return Başvuruların listesi.
     */
    suspend fun getWorkerApplications(workerId: String): List<Application> {
        try {
            val response: PostgrestResult = supabase
                .from("applications")
                .select {
                    filter { eq("worker_id", workerId) }
                }
            return response.decodeList<Application>()
        } catch (e: Exception) {
            Log.e("RepoDebug", "getWorkerApplications HATA: ${e.message}", e)
            return emptyList()
        }
    }

    /**
     * İşçinin belirli bir işe yaptığı başvuruyu iptal eder (applications tablosundan siler).
     * @param jobId Başvurusu iptal edilecek işin ID'si.
     * @param workerId Başvurusu iptal edilecek işçinin ID'si.
     * @return Başarılıysa true, değilse false.
     */
    suspend fun cancelApplication(jobId: String, workerId: String): Boolean {
        return try {
            Log.d("RepoDebug", "cancelApplication: Başvuru siliniyor. JobID: $jobId, WorkerID: $workerId")
            supabase.from("applications")
                .delete {
                    filter {
                        eq("job_id", jobId)
                        eq("worker_id", workerId) // Parametreden gelen ID'yi kullan
                    }
                }
            Log.i("RepoDebug", "cancelApplication: Başvuru BAŞARIYLA iptal edildi.")
            true
        } catch (e: Exception) {
            Log.e("RepoDebug", "cancelApplication HATA: ${e.message}", e)
            false
        }
    }

    /**
     * Bir iş ilanına yapılmış TÜM başvuruları getirir.
     * Bu fonksiyon sadece application tablosunu değil,
     * başvuran işçinin profil bilgilerini de (join mantığıyla) getirmelidir.
     * Şimdilik basitlik adına önce başvuruları, sonra profilleri çekeceğiz.
     */
    suspend fun getApplicationsForJob(jobId: String): List<Pair<Application, User>> {
        Log.d("RepoDebug", "getApplicationsForJob: Başvurular çekiliyor... JobID: $jobId")
        try {
            // 1. Başvuruları çek
            val appsResponse = supabase.from("applications").select {
                filter { eq("job_id", jobId) }
            }
            val applications = appsResponse.decodeList<Application>()

            if (applications.isEmpty()) return emptyList()

            // 2. Başvuran işçilerin ID'lerini topla
            val workerIds = applications.map { it.workerId }

            // 3. Bu ID'lere sahip kullanıcı profillerini çek
            val usersResponse = supabase.from("users").select {
                filter { isIn("id", workerIds) }
            }
            val workers = usersResponse.decodeList<User>()
            val workersMap = workers.associateBy { it.id }

            // 4. Başvuru + İşçi Profili çifti oluştur
            val result = applications.mapNotNull { app ->
                val worker = workersMap[app.workerId]
                if (worker != null) {
                    Pair(app, worker)
                } else null
            }

            Log.i("RepoDebug", "getApplicationsForJob: ${result.size} başvuru ve profil eşleşti.")
            return result

        } catch (e: Exception) {
            Log.e("RepoDebug", "getApplicationsForJob HATA: ${e.message}", e)
            return emptyList()
        }
    }

    /**
     * Başvuru durumunu günceller (approved/rejected).
     */
    suspend fun updateApplicationStatus(applicationId: String, newStatus: String): Boolean {
        return try {
            Log.d("RepoDebug", "updateApplicationStatus: Durum güncelleniyor -> $newStatus")

            supabase.from("applications").update(
                mapOf("status" to newStatus)
            ) {
                filter { eq("id", applicationId) }
            }

            Log.i("RepoDebug", "updateApplicationStatus: Başarılı.")
            true
        } catch (e: Exception) {
            Log.e("RepoDebug", "updateApplicationStatus HATA: ${e.message}", e)
            false
        }
    }

    suspend fun getReviewsForUser(userId: String): List<ReviewWithReviewer> {
        return try {
            // Reviews tablosundan veriyi çek
            val response = supabase.from("reviews").select {
                filter { eq("reviewee_id", userId) }
                order("created_at", Order.DESCENDING) // En yeniler üstte
                limit(10) // Son 10 yorum yeterli
            }

            val reviews = response.decodeList<Review>()

            if (reviews.isEmpty()) return emptyList()

            // Yorum yapanların isimlerini bulmak için user tablosuna git
            val reviewerIds = reviews.map { it.reviewerId }
            val usersResponse = supabase.from("users").select {
                filter { isIn("id", reviewerIds) }
            }
            val reviewers = usersResponse.decodeList<User>()
            val reviewersMap = reviewers.associateBy { it.id }

            // Yorum + İsim birleştir
            reviews.map { review ->
                ReviewWithReviewer(
                    review = review,
                    reviewerName = reviewersMap[review.reviewerId]?.name ?: "Anonim"
                )
            }
        } catch (e: Exception) {
            Log.e("RepoDebug", "getReviewsForUser HATA: ${e.message}")
            emptyList()
        }
    }

    suspend fun submitReview(
        jobId: String,
        revieweeId: String, // Kime puan veriliyor? (İşçi -> İşveren ID'si)
        score: Int,
        comment: String
    ): Boolean {
        val currentUserId = supabase.auth.currentUserOrNull()?.id ?: return false
        return try {
            Log.d("RepoDebug", "submitReview: Puan gönderiliyor... Kimden: $currentUserId Kime: $revieweeId İş: $jobId")
            val review = Review(
                jobId = jobId,
                reviewerId = currentUserId,
                revieweeId = revieweeId,
                score = score,
                comment = comment
            )
            supabase.from("reviews").insert(review)
            Log.i("RepoDebug", "submitReview: BAŞARILI.")
            true
        } catch (e: Exception) {
            // Eğer zaten puan vermişse veritabanı "unique constraint" hatası verir
            Log.e("RepoDebug", "submitReview HATA: ${e.message}", e)
            false
        }
    }

    /** Kullanıcının yazdığı TÜM yorumları getirir (Hangi işlere yorum yapmışım?) */
    suspend fun getMyReviews(reviewerId: String): List<Review> {
        return try {
            val response = supabase.from("reviews").select {
                filter { eq("reviewer_id", reviewerId) }
            }
            response.decodeList<Review>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Verilen ID listesine sahip kullanıcıları (İşverenleri) getirir (Puanları için) */
    suspend fun getUsersByIds(userIds: List<String>): List<User> {
        return try {
            val response = supabase.from("users").select {
                filter { isIn("id", userIds) }
            }
            response.decodeList<User>()
        } catch (e: Exception) {
            emptyList()
        }
    }
    suspend fun getUserProfile(userId: String): User? {
        return try {
            val response = supabase.from("users").select {
                filter { eq("id", userId) }
            }
            response.decodeList<User>().firstOrNull()
        } catch (e: Exception) {
            null
        }
    }
    suspend fun getJobById(jobId: String): Job? {
        return try {
            supabase.from("jobs").select { filter { eq("id", jobId) } }
                .decodeSingle<Job>()
        } catch (e: Exception) { null }
    }
    suspend fun getReviewsByJobAndReviewer(jobId: String, reviewerId: String): List<Review> {
        return try {
            val response = supabase.from("reviews").select {
                filter {
                    eq("job_id", jobId)
                    eq("reviewer_id", reviewerId)
                }
            }
            response.decodeList<Review>()
        } catch (e: Exception) {
            emptyList()
        }
    }

}
