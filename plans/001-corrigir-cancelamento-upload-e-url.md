# 001 — Corrigir cancelamento de upload e geração de URL

## Por que isso importa

**Cancelamento fantasma:** Quando o usuário toca "Cancelar" durante um upload, o estado do app muda para `CANCELLED` mas a requisição HTTP continua rodando em segundo plano. O usuário acha que cancelou, mas o upload ainda está consumindo dados móveis e bateria.

**URL incorreta:** O app constrói o link do arquivo manualmente usando o nome do arquivo (`https://buzzheavier.com/app-release-unsigned.apk`), mas a API retorna um ID único no JSON de resposta. O link correto deve usar esse ID (`https://buzzheavier.com/fivxbj8yb6jz`). Isso quebra o compartilhamento — o link gerado atualmente não funciona.

## Arquivos envolvidos

- `app/src/main/java/com/buzzheavier/uploader/network/UploadManager.kt` (todo o fluxo)
- `app/src/main/java/com/buzzheavier/uploader/data/Models.kt` (UploadResult já tem `fileId`)
- `app/src/main/java/com/buzzheavier/uploader/UploadConstants.kt` (opcional — adicionar constantes)

## Estado atual (código relevante)

### Cancelamento — `UploadManager.kt:183-185`

```kotlin
fun cancelUpload() {
    _uploadState.value = _uploadState.value.copy(status = UploadStatus.CANCELLED)
}
```

Só muda o estado. Não aborta a chamada HTTP. O `OkHttpClient` tem `dispatcher.cancelAll()` e cada `Call` tem `call.cancel()`, mas nenhum é invocado.

### Geração de URL — `UploadManager.kt:150-158`

```kotlin
if (response.isSuccessful) {
    val responseBody = response.body?.string() ?: ""
    val fileUrl = "https://buzzheavier.com/$fileName"
    val result = UploadResult(
        success = true,
        url = fileUrl,
        fileName = fileName,
        fileSize = fileSize
    )
```

O `responseBody` contém o JSON de resposta da API, por exemplo:
```json
{"code":201,"data":{"id":"fivxj8yb6jz","name":"app-release-unsigned.apk",...}}
```

O campo `data.id` deve ser extraído e usado para montar a URL:
```
https://buzzheavier.com/fivxj8yb6jz
```

O model `UploadResult` já tem o campo `fileId` — basta preenchê-lo.

## Passos

### Passo 1 — Adicionar modelo para resposta da API de upload

Criar uma data class em `Models.kt` para parsear a resposta:

```kotlin
data class UploadApiResponse(
    val code: Int,
    val data: UploadData
)

data class UploadData(
    val id: String,
    val name: String,
    val size: Long,
    val expiry: String
)
```

Colocar ao lado das outras data classes em `Models.kt`.

### Passo 2 — Referenciar `Call` no `UploadManager` para permitir cancelamento real

Em `UploadManager.kt`:

1. Adicionar um campo `private var currentCall: Call? = null`
2. Antes de executar `client.newCall(request).execute()`, salvar a call:
   ```kotlin
   val call = client.newCall(request)
   currentCall = call
   val response = call.execute()
   ```
3. No método `cancelUpload()`:
   ```kotlin
   fun cancelUpload() {
       currentCall?.cancel()
       currentCall = null
       _uploadState.value = _uploadState.value.copy(status = UploadStatus.CANCELLED)
   }
   ```
4. No `resetState()`, limpar `currentCall = null`

### Passo 3 — Extrair `id` da resposta e usar na URL

No bloco `if (response.isSuccessful)` de `UploadManager.kt:150-158`, substituir:

```kotlin
if (response.isSuccessful) {
    val responseBody = response.body?.string() ?: ""
    val fileUrl = "https://buzzheavier.com/$fileName"
    val result = UploadResult(
        success = true,
        url = fileUrl,
        fileName = fileName,
        fileSize = fileSize
    )
```

Por:

```kotlin
if (response.isSuccessful) {
    val responseBody = response.body?.string() ?: ""
    val apiResponse = gson.fromJson(responseBody, UploadApiResponse::class.java)
    val fileId = apiResponse?.data?.id ?: ""
    val fileUrl = "https://buzzheavier.com/$fileId"
    val result = UploadResult(
        success = true,
        url = fileUrl,
        fileName = fileName,
        fileSize = fileSize,
        fileId = fileId
    )
```

**Nota:** Você precisa adicionar uma instância de `Gson` no `UploadManager` (igual já existe em `BuzzHeavierApi`):
```kotlin
private val gson = Gson()
```

Adicionar também os imports: `import com.google.gson.Gson` e `import com.buzzheavier.uploader.data.UploadApiResponse` (ou onde os models forem colocados).

### Passo 4 — Verificar se o import de Gson já existe

O `UploadManager.kt` atualmente **não** importa `Gson`. Adicionar `import com.google.gson.Gson` no topo do arquivo.

### Passo 5 — Garantir que os campos extras do response também sejam parseados

O `UploadApiResponse.data` deve conter ao menos `id` e `name`. Os campos extras do JSON (`size`, `expiry`, etc.) são ignorados pelo Gson quando não declarados, então não há risco.

## Critérios de sucesso (verificáveis)

1. `./gradlew assembleDebug --stacktrace --no-daemon` compila sem erros
2. `UploadManager.cancelUpload()` agora invoca `currentCall?.cancel()` — verificar por inspeção de código
3. O link gerado após upload bem-sucedido usa o formato `https://buzzheavier.com/{id}` em vez de `https://buzzheavier.com/{fileName}`
4. `UploadResult.fileId` contém o valor extraído do JSON de resposta

## Limites de escopo

- **Não** mexer no `BuzzHeavierApi.kt` (só `UploadManager.kt` e `Models.kt`)
- **Não** alterar assinaturas de métodos públicos
- **Não** introduzir Retrofit ou mudar a arquitetura de rede

## Plano de testes

Não há testes existentes (ver finding F4). Após as mudanças, criar um teste unitário simples em `app/src/test/java/com/buzzheavier/uploader/network/UploadManagerTest.kt`:

```kotlin
class UploadManagerTest {
    @Test
    fun `cancelUpload should abort HTTP call`() {
        // Verificar que currentCall?.cancel() é chamado
        // usando um mock de Call
    }

    @Test
    fun `success response should parse fileId from JSON`() {
        val json = """{"code":201,"data":{"id":"abc123","name":"test.txt","size":100}}"""
        val response = Gson().fromJson(json, UploadApiResponse::class.java)
        assertEquals("abc123", response.data.id)
    }
}
```

## Nota de manutenção

- Se no futuro a API mudar o formato de resposta, o `UploadApiResponse` precisará ser atualizado
- Se o `UploadManager` for substituído por `WorkManager`, a lógica de cancelamento deverá migrar para usar `WorkManager.cancelWorkById()`

## Escape hatches

- Se o `response.body?.string()` retornar `null` ou JSON inválido, o `gson.fromJson` retornará `null` — o código deve tratar isso gracefully (manter `fileId` vazio e usar um fallback)
- Se o campo `data.id` não existir na resposta (API antiga), a URL deve manter o comportamento anterior como fallback
