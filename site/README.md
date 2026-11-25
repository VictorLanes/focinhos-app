# Anjos de Focinho: site

Um único arquivo (`index.html`), responsivo para celular e computador. Para publicar, hospede em qualquer lugar:
GitHub Pages, Netlify ou Google Apps Script (HtmlService).

## Onde ficam os dados
Por enquanto os dados ficam no navegador (`localStorage`, chave `anjos_de_focinho_v1`).
O formato é o mesmo do backup do app Android: `pets`, `registros` (valor em centavos, datas `aaaa-mm-dd`),
`lembretes` e `fotos` (base64). Em **Ajustes → Backup** dá para levar os dados do app para o site e vice-versa.

## Integração (para depois)
No início do script existe o objeto `Integracao`:

```js
const Integracao = {
  salvar(estado) {},          // chamado a cada alteração, com o estado completo
  carregar() { return null; } // chamado ao abrir: devolva o estado vindo do servidor
};
```

Basta trocar essas duas funções por chamadas à sua API (por exemplo, `fetch` para um Web App do Apps Script que grava no Google Sheets).
