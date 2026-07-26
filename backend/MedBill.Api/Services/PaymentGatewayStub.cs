using System.Security.Cryptography;
using System.Text;

namespace MedBill.Api.Services;

public class PaymentGatewayStub : IPaymentGateway
{
    public Task<(string sessionId, string redirectUrl)> CreatePaymentSessionAsync(Guid billId, decimal amount, string currency, CancellationToken cancellationToken)
    {
        var sessionId = $"sess_{Guid.NewGuid():N}";
        var redirectUrl = $"https://payments.example.com/checkout/{sessionId}?bill={billId}";
        return Task.FromResult((sessionId, redirectUrl));
    }

    public bool ValidateWebhook(string payload, string signature, string sharedSecret)
    {
        var key = Encoding.UTF8.GetBytes(sharedSecret);
        var content = Encoding.UTF8.GetBytes(payload);
        using var hmac = new HMACSHA256(key);
        var computed = Convert.ToHexString(hmac.ComputeHash(content));
        return string.Equals(computed, signature, StringComparison.OrdinalIgnoreCase);
    }
}
