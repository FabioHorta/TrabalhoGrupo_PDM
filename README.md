# README Técnico – EcoTrack (Aplicação Android)

## 1. Visão Geral
O EcoTrack é uma aplicação Android destinada a monitorizar consumos energéticos, registar leituras, gerir casas e eletrodomésticos, comunicar com técnicos e agendar assistências.  
Foi concebida com um modelo offline-first, permitindo funcionamento mesmo sem internet e sincronização automática quando a ligação é retomada.

## 2. Arquitectura da Aplicação
- Activities que representam cada módulo.
- DBHelper com SQLite para armazenamento local.
- Retrofit + JWT para comunicação segura.
- SyncUtils para sincronização bidireccional com o servidor Node.js.

## 3. Base de Dados Local (SQLite)
Estruturas principais:
- users
- casas
- appliances
- leituras
- mensagens_chat
- mensagens_suporte
- assistencias
- tecnicos  
Inclui flags de sync_status para evitar duplicações.

## 4. Fluxo de Autenticação
- Login via email/password.
- Login Google OAuth.
- JWT guardado em SharedPreferences.
- Token enviado em cada pedido Retrofit.

## 5. Sincronização (SyncUtils)
Sincronização automática sempre que há internet:
- Leituras
- Appliances
- Casas
- Assistências
- Mensagens de suporte
- Chat  
Também restaura dados do servidor quando a BD local está vazia.

## 6. Gestão de Leituras
- Fotografias guardadas localmente.
- Envio da leitura + envio posterior da imagem em Base64.
- Restauro cria novamente os ficheiros locais.

## 7. Módulo de Assistências
- Agendamento.
- Quando a internet regressa, sincroniza com servidor.
- Técnicos atribuídos automaticamente se estiverem livres.

## 8. Sistema de Chat
- Sincroniza ao voltar online.
- Mensagens têm timestamp único.
- Sync evita duplicados e faz merge inteligente.

## 9. Alertas de Consumo
- Baseado em média dos últimos consumos.
- Categorias: alto, baixo, normal, início.
- Funciona offline com cache local e tenta actualizar se houver internet.

## 10. Relatórios Técnicos
- Técnicos podem gerar PDF (via servidor).
- Conteúdo é devolvido em Base64 e pode ser guardado ou visualizado.

## 11. Comunicação com o Servidor
- Retrofit configurado para usar sempre HTTPS/ngrok.
- Interceptor adiciona token JWT a cada pedido.

## 12. Segurança
- Tokens JWT.
- HTTPS obrigatório.
- Sincronização valida sempre permissões (servidor recusa dados inválidos).
