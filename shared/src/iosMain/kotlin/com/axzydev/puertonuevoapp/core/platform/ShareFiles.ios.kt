package com.axzydev.puertonuevoapp.core.platform

/**
 * Stub de iOS (Fase 1, MVP Android-first): compartir archivos se implementa en
 * la Fase 4, así que por ahora no hace nada (las exportaciones CSV en iOS aún
 * no están disponibles).
 *
 * TODO(Fase 4): escribir los bytes en `NSTemporaryDirectory()`, crear el
 * `NSURL` y presentar `UIActivityViewController` desde la `UIViewController`
 * de Compose; en iPad configurar `popoverPresentationController.sourceView`
 * para que no reviente. Mismo contrato que Android.
 */
actual fun shareFile(name: String, mime: String, bytes: ByteArray) {
    // TODO(Fase 4): UIActivityViewController.
}
