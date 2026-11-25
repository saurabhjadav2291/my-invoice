package com.revishingstudio.invoice.core

/**
 * The client, business and bank details an invoice was sent with.
 *
 * Drafts follow the latest details. Once an invoice is sent its details are frozen
 * here, so editing a client's address or your bank later never changes an invoice
 * that has already gone out.
 */
data class InvoiceSnapshot(
    val client: Client,
    /** Null when no business profile existed yet; the latest one is used then. */
    val issuer: BusinessProfile?,
    val bank: BankAccount?,
) {
    /** One "key=value" per line; backslash, newline and tab in values are escaped. */
    fun encode(): String = buildList {
        add("v" to "1")
        add("c.name" to client.name)
        add("c.legal" to client.legalName)
        add("c.address" to client.address)
        add("c.phone" to client.phone)
        add("c.email" to client.email)
        add("c.gstin" to client.gstin)
        add("c.tax" to TaxIds.encode(client.taxFields))
        add("c.currency" to client.defaultCurrency.code)
        issuer?.let { p ->
            add("p.owner" to p.ownerName)
            add("p.studio" to p.studioName)
            add("p.address" to p.address)
            add("p.pan" to p.pan)
            add("p.email" to p.email)
            add("p.phone" to p.phone)
            p.logoPath?.let { add("p.logo" to it) }
            add("p.gstnote" to p.showGstNote.toString())
            add("p.duedate" to p.showDueDate.toString())
        }
        bank?.let { b ->
            add("b.id" to b.id.toString())
            add("b.label" to b.label)
            add("b.bank" to b.bankName)
            add("b.name" to b.accountName)
            add("b.number" to b.accountNumber)
            add("b.ifsc" to b.ifsc)
            add("b.swift" to b.swift)
        }
    }.joinToString("\n") { (k, v) -> "$k=${escape(v)}" }

    companion object {
        /** Reads [text]; [clientId] is kept on the client so it still links to the live record. */
        fun decode(text: String, clientId: Long): InvoiceSnapshot {
            val map = text.lineSequence()
                .filter { '=' in it }
                .associate { line -> line.substringBefore('=') to unescape(line.substringAfter('=')) }
            fun s(key: String) = map[key].orEmpty()
            val client = Client(
                id = clientId,
                name = s("c.name"),
                legalName = s("c.legal"),
                address = s("c.address"),
                phone = s("c.phone"),
                email = s("c.email"),
                gstin = s("c.gstin"),
                taxFields = TaxIds.decode(s("c.tax")),
                defaultCurrency = Currency.entries.firstOrNull { it.code == s("c.currency") } ?: Currency.INR,
            )
            val issuer = if ("p.owner" in map) {
                BusinessProfile(
                    ownerName = s("p.owner"),
                    studioName = s("p.studio"),
                    address = s("p.address"),
                    pan = s("p.pan"),
                    email = s("p.email"),
                    phone = s("p.phone"),
                    logoPath = map["p.logo"],
                    showGstNote = s("p.gstnote") != "false",
                    showDueDate = s("p.duedate") != "false",
                )
            } else {
                null
            }
            val bank = if ("b.label" in map) {
                BankAccount(
                    id = s("b.id").toLongOrNull() ?: 0,
                    label = s("b.label"),
                    bankName = s("b.bank"),
                    accountName = s("b.name"),
                    accountNumber = s("b.number"),
                    ifsc = s("b.ifsc"),
                    swift = s("b.swift"),
                )
            } else {
                null
            }
            return InvoiceSnapshot(client, issuer, bank)
        }

        private fun escape(v: String) = v.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t").replace("\r", "")

        private fun unescape(v: String): String = buildString {
            var i = 0
            while (i < v.length) {
                val ch = v[i]
                if (ch == '\\' && i + 1 < v.length) {
                    when (v[i + 1]) {
                        'n' -> append('\n')
                        't' -> append('\t')
                        else -> append(v[i + 1])
                    }
                    i += 2
                } else {
                    append(ch)
                    i++
                }
            }
        }
    }
}
