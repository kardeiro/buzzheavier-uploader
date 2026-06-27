# Planos de Melhoria — buzzheavier-uploader

HEAD: `34f2712`

## Índice

| # | Plano | Prioridade | Status |
|---|---|---|---|
| 001 | Corrigir cancelamento de upload e geração de URL | P0 | pendente |
| 002 | Singleton OkHttpClient | P1 | pendente |
| 003 | Verificação de rede antes do upload | P2 | pendente |
| 004 | Remover `recreate()` do `onNewIntent` | P2 | pendente |
| 005 | Validação de tamanho de arquivo | P3 | pendente |
| 006 | Testes de unidade (baseline) | P0 | pendente |

## Dependências

- `006` (testes) deve ser executado antes de qualquer refatoração que mexa em lógica de negócio
- `001` e `002` são independentes entre si
- `003` depende de `002` (singleton cliente HTTP + verificação de rede)
- `004` é independente
- `005` é independente

## Ordem recomendada

1. `001` — bug real de cancelamento + URL quebrada (P0)
2. `002` — performance/boas práticas (P1)
3. `003` + `004` — UX (P2)
4. `005` — validação (P3)
5. `006` — testes (deve vir antes de refatorações maiores, mas é esforço G)
