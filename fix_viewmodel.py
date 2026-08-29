import re

with open("app/src/main/java/com/example/ui/AgentViewModel.kt", "r") as f:
    content = f.read()

imports = """import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
"""

content = content.replace("import kotlinx.coroutines.launch\n", imports + "import kotlinx.coroutines.launch\n")

state_additions = """
    private val _authMode = MutableStateFlow("API_KEY")
    val authMode: StateFlow<String> = _authMode.asStateFlow()

    private val _googleAccountEmail = MutableStateFlow<String?>(null)
    val googleAccountEmail: StateFlow<String?> = _googleAccountEmail.asStateFlow()
    
    private val _googleIdToken = MutableStateFlow<String?>(null)

    private val _googleProjectId = MutableStateFlow<String?>(null)
    val googleProjectId: StateFlow<String?> = _googleProjectId.asStateFlow()

    private val _availableProjects = MutableStateFlow<List<String>>(emptyList())
    val availableProjects: StateFlow<List<String>> = _availableProjects.asStateFlow()
"""
content = content.replace("private val _showSettings = MutableStateFlow(false)", state_additions + "\n    private val _showSettings = MutableStateFlow(false)")

functions_additions = """
    fun setAuthMode(mode: String) {
        _authMode.value = mode
    }

    fun setGoogleProjectId(projectId: String) {
        _googleProjectId.value = projectId
    }

    fun signInWithGoogle(context: Context, serverClientId: String) {
        viewModelScope.launch {
            try {
                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(serverClientId)
                    .setAutoSelectEnabled(true)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                
                if (credential is androidx.credentials.CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    _googleAccountEmail.value = googleIdTokenCredential.id
                    _googleIdToken.value = googleIdTokenCredential.idToken
                    fetchGoogleCloudProjects(googleIdTokenCredential.idToken)
                }
            } catch (e: Exception) {
                _terminalLogs.value += "\\nGoogle Sign-In Failed: ${e.message}"
            }
        }
    }

    private fun fetchGoogleCloudProjects(token: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://cloudresourcemanager.googleapis.com/v1/projects")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.setRequestProperty("Accept", "application/json")

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)
                    val projects = json.optJSONArray("projects")
                    val projectList = mutableListOf<String>()
                    if (projects != null) {
                        for (i in 0 until projects.length()) {
                            val project = projects.getJSONObject(i)
                            projectList.add(project.getString("projectId"))
                        }
                    }
                    _availableProjects.value = projectList
                    if (projectList.isNotEmpty() && _googleProjectId.value == null) {
                        _googleProjectId.value = projectList.first()
                    }
                } else {
                    _terminalLogs.value += "\\nFailed to fetch projects. Code: ${connection.responseCode}"
                }
            } catch (e: Exception) {
                _terminalLogs.value += "\\nError fetching projects: ${e.message}"
            }
        }
    }
"""
content = content.replace("fun initPrefs(context: Context) {", functions_additions + "\n    fun initPrefs(context: Context) {")

with open("app/src/main/java/com/example/ui/AgentViewModel.kt", "w") as f:
    f.write(content)

