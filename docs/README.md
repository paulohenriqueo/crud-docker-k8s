# docs

Documentação explicativa do POC.

Existe porque o objetivo do projeto tem duas metades: **construir** o ambiente e **explicar o
papel e a função de cada componente envolvido**. Um cluster que sobe mas que ninguém sabe
descrever não cumpre o segundo, e é o segundo que se defende numa conversa.

Cada documento é escrito **junto** com o bloco correspondente do roadmap, não no final. Escrever
depois vira transcrição do que ficou pronto; escrever junto obriga a entender enquanto se faz —
e as dúvidas que aparecem no caminho são exatamente o conteúdo que falta.

| Documento | Responde |
|---|---|
| `01-containers.md` | Por que multi-stage, o que fica de fora da imagem final, o que muda de Compose para K8s |
| `02-componentes-kubernetes.md` | O que faz cada peça do cluster e o que acontece quando se roda `kubectl apply` |
| `03-comunicacao.md` | Como os serviços se encontram: Service, DNS interno, ClusterIP |
| `04-resiliencia.md` | O que o cluster faz quando algo cai — com o resultado observado, não o esperado |
| `05-seguranca.md` | RBAC, ServiceAccount, NetworkPolicy, Pod Security Standards, e os limites do Secret |
| `06-custo.md` | O que custa dinheiro num cluster e como requests/limits entram nisso |

## Regra de escrita

**Todo comportamento afirmado aqui foi observado, não deduzido do manifesto.** O documento traz
o comando que produziu a evidência, para qualquer pessoa reproduzir. Comportamento que só foi
lido na documentação oficial e não testado aqui é marcado como tal — a diferença entre "eu li"
e "eu vi" é a diferença entre repetir e saber.
