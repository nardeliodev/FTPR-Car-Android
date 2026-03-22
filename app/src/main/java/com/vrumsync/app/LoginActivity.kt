package com.vrumsync.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.vrumsync.app.databinding.ActivityLoginBinding
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private var verificationId = ""

    // ADICIONADO → controle de loading
    private fun setLoading(isLoading: Boolean) {
        binding.btnSendSms.isEnabled = !isLoading
        binding.btnVerifySms.isEnabled = !isLoading
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        setupView()
    }

    override fun onResume() {
        super.onResume()
        verifyLoggedUser()
    }

    private fun verifyLoggedUser() {
        if (auth.currentUser != null) {
            navigateToMainActivity()
        }
    }

    private fun setupView() {
        binding.btnSendSms.setOnClickListener { sendVerificationCode() }
        binding.btnVerifySms.setOnClickListener { verifyCode() }
    }

    private fun sendVerificationCode() {

        val rawPhone = binding.cellphone.text.toString()

        // ADICIONADO → NORMALIZAÇÃO
        val phoneNumber = normalizePhone(rawPhone)

        // MELHORADO → VALIDAÇÃO
        if (!isValidPhone(phoneNumber)) {
            binding.cellphone.error = "Formato inválido (+5511999999999)"
            return
        }

        setLoading(true)

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(45L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    auth.signInWithCredential(credential)
                        .addOnCompleteListener { onCredentialCompleteListener(it) }
                }

                override fun onVerificationFailed(exception: FirebaseException) {

                    setLoading(false)

                    // ADICIONADO → CORREÇÃO DO BUG
                    val message = exception.localizedMessage ?: ""

                    when {

                        // ADICIONADO → TRATAR BILLING
                        message.contains("BILLING_NOT_ENABLED") -> {
                            Toast.makeText(
                                this@LoginActivity,
                                "Ative o faturamento ou use número de teste",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        exception is FirebaseAuthInvalidCredentialsException -> {
                            binding.cellphone.error = "Número inválido"
                        }

                        exception is FirebaseTooManyRequestsException -> {
                            Toast.makeText(
                                this@LoginActivity,
                                "Muitas tentativas. Tente mais tarde.",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        else -> {
                            Toast.makeText(
                                this@LoginActivity,
                                "Erro: $message",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    setLoading(false)

                    this@LoginActivity.verificationId = verificationId

                    binding.veryfyCode.visibility = View.VISIBLE
                    binding.btnVerifySms.visibility = View.VISIBLE

                    Toast.makeText(
                        this@LoginActivity,
                        "Código enviado!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun verifyCode() {
        val code = binding.veryfyCode.text.toString().trim()

        if (verificationId.isEmpty()) {
            Toast.makeText(this, "Envie o SMS primeiro", Toast.LENGTH_SHORT).show()
            return
        }

        if (code.isEmpty()) {
            binding.veryfyCode.error = "Digite o código"
            return
        }

        setLoading(true)

        val credential = PhoneAuthProvider.getCredential(verificationId, code)

        auth.signInWithCredential(credential)
            .addOnCompleteListener { onCredentialCompleteListener(it) }
    }

    private fun onCredentialCompleteListener(task: Task<AuthResult>) {

        setLoading(false)

        if (task.isSuccessful) {
            navigateToMainActivity()
        } else {

            val message = when (task.exception) {
                is FirebaseAuthInvalidCredentialsException -> "Código inválido"
                else -> task.exception?.localizedMessage ?: "Erro ao autenticar"
            }

            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun navigateToMainActivity() {
        startActivity(MainActivity.newIntent(this))
        finish()
    }

    // ADICIONADO → NORMALIZAÇÃO
    private fun normalizePhone(phone: String): String {
        return phone.replace("[^\\d+]".toRegex(), "")
    }

    // MELHORADO → VALIDAÇÃO BRASIL
    private fun isValidPhone(phone: String): Boolean {
        val regex = Regex("^\\+55\\d{10,11}$")
        return regex.matches(phone)
    }

    companion object {
        fun newIntent(context: Context) =
            Intent(context, LoginActivity::class.java)
    }
}