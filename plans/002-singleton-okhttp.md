# 002 — Singleton OkHttpClient

## Por que isso importa

Atualmente, cada instância de `UploadManager` e `BuzzHeavierApi` cria seu próprio `OkHttpClient` com um novo pool de conexões, pool de threads (`dispatcher.executorService`), e configurações de timeout. Com 3 ViewModels ativos, existem 3+ pools de conexão rodando em paralelo. Isso:

- Desperdiça threads nativas (cada pool cria threads de background)
- Perde o reaproveitamento de conexões HTTP Keep-Alive (cada cliente tem seu pool独立)
- Dificulta a configuração centralizada (ex: adicionar um interceptor de logging ou auth uma vez só)

## Arquivos envolvidos

- `app/src/main/java/com/buzzheavier/uploader/network/BuzzHeavierApi.kt` (linhas 20-24)
- `app/src/main/java/com/buzzheavier/uploader/network/UploadManager.kt` (linhas 26-30)
- Criar (opcional): `app/src/main/java/com/buzzheavier/uploader/network/HttpClientProvider.kt`

## Estado atual

### `BuzzHeavierApi.kt:20-24`
```kotlin
private val client = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .writeTimeout(300, TimeUnit.SECONDS)
    .build()
```

### `UploadManager.kt:26-30`
```kotlin
private val client = OkHttpClient.Builder()
    .connectTimeout(60, TimeUnit.SECONDS)
    .readTimeout(300, TimeUnit.SECONDS)
    .writeTimeout(600, TimeUnit.SECONDS)
    .build()
```

**Problema:** cada classe cria seu próprio `OkHttpClient`. Note que os timeouts são **diferentes** entre as duas classes — `UploadManager` tem timeouts mais generosos (upload de arquivos grandes). Isso é um requisito legítimo, então a solução precisa permitir clientes com configurações diferentes, mas compartilhando o mesmo pool de conexões e dispatcher.

## Solução

Criar um `object HttpClientProvider` que expõe um `OkHttpClient` base com configurações compartilhadas (pool de conexões, dispatcher, interceptors). Cada classe então cria seu cliente customizado a partir desse base usando `client.newBuilder()`.

## Passos

### Passo 1 — Criar `HttpClientProvider.kt`

Arquivo: `app/src/main/java/com/buzzheavier/uploader/network/HttpClientProvider.kt`

```kotlin
package com.buzzheavier.uploader.network

import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor

object HttpClientProvider {

    val baseClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(300, TimeUnit.SECONDS)
            .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
            .dispatcher(Dispatcher(
                ThreadPoolExecutor(
                    0, Int.MAX_VALUE, 60, TimeUnit.SECONDS,
                    SynchronousQueue()
                )
            ))
            .build()
    }
}
```

Explicação breve:
- `ConnectionPool(5, 5, MINUTES)` — mantém até 5 conexões Keep-Alive por 5 minutos
- `Dispatcher` com `ThreadPoolExecutor` — reusa threads em vez de criar uma por requisição
- `by lazy` — o cliente é criado uma única vez na JVM

### Passo 2 — Atualizar `BuzzHeavierApi.kt` para usar o `baseClient`

Substituir:

```kotlin
private val client = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .writeTimeout(300, TimeUnit.SECONDS)
    .build()
```

Por:

```kotlin
private val client = HttpClientProvider.baseClient.newBuilder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .writeTimeout(300, TimeUnit.SECONDS)
    .build()
```

### Passo 3 — Atualizar `UploadManager.kt` para usar o `baseClient`

Substituir:

```kotlin
private val client = OkHttpClient.Builder()
    .connectTimeout(60, TimeUnit.SECONDS)
    .readTimeout(300, TimeUnit.SECONDS)
    .writeTimeout(600, TimeUnit.SECONDS)
    .build()
```

Por:

```kotlin
private val client = HttpClientProvider.baseClient.newBuilder()
    .connectTimeout(60, TimeUnit.SECONDS)
    .readTimeout(300, TimeUnit.SECONDS)
    .writeTimeout(600, TimeUnit.SECONDS)
    .build()
```

Note que cada classe mantém seus timeouts específicos — a única coisa compartilhada é o pool de conexões e o dispatcher (via `newBuilder()`).

### Passo 4 — Verificar imports

Ambos os arquivos já importam `OkHttpClient` e `TimeUnit`. Adicionar import de `HttpClientProvider` se necessário (está no mesmo pacote `network`, então não precisa).

## Critérios de sucesso (verificáveis)

1. `./gradlew assembleDebug --stacktrace --no-daemon` compila sem erros
2. `HttpClientProvider.baseClient` é a mesma instância (singleton) — verificável adicionando um `require()` no construtor ou printando `identityHashCode`
3. `BuzzHeavierApi` e `UploadManager` ainda têm seus timeouts individuais preservados
4. Nenhuma mudança no comportamento externo das classes

## Limites de escopo

- **Não** criar um sistema de DI (Hilt/Koin) — isso viria em um plano futuro separado
- **Não** mexer no `UploadManager.cancelUpload()` — isso é escopo do plano 001
- **Não** adicionar interceptors (logging, auth) — apenas preparar o terreno

## Plano de testes

Não há testes existentes. Após a mudança, adicionar em `app/src/test/java/com/buzzheavier/uploader/network/HttpClientProviderTest.kt`:

```kotlin
class HttpClientProviderTest {
    @Test
    fun `baseClient is a singleton`() {
        val instance1 = HttpClientProvider.baseClient
        val instance2 = HttpClientProvider.baseClient
        assertSame(instance1, instance2)
    }

    @Test
    fun `newBuilder preserves timeouts`() {
        val custom = HttpClientProvider.baseClient.newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .build()
        assertEquals(60, custom.connectTimeoutMillis / 1000)
    }
}
```

## Nota de manutenção

- Se um interceptor global for necessário (ex: logging, auth refresh header), adicione no `baseClient` em `HttpClientProvider` — toda classe que usa `baseClient.newBuilder()` herdará o interceptor
- Se surgir uma terceira classe que precise de HTTP, ela deve seguir o mesmo padrão: `HttpClientProvider.baseClient.newBuilder().configurarTimeouts().build()`
- O `Dispatcher` compartilhado significa que uma rajada de requests do `BuzzHeavierApi` pode consumir threads que o `UploadManager` precisaria — mas na prática o upload é sequencial, então não há contenção real

## Escape hatches

- Se por algum motivo o `by lazy` não funcionar (testes multi-thread), trocar por `@Volatile` + double-checked locking ou usar um `Lock`
- Se for necessário resetar o pool de conexões (ex: mudança de rede), expor um método `fun resetConnectionPool()` no `HttpClientProvider`
