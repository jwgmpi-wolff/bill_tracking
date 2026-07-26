using MedBill.Api.Data;
using MedBill.Api.Services;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;

var builder = WebApplication.CreateBuilder(args);
var startupState = new StartupState();

builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();
builder.Services.AddSingleton(startupState);

builder.Services.AddDbContext<AppDbContext>(options =>
    options.UseSqlServer(builder.Configuration.GetConnectionString("Sql")));

builder.Services.AddScoped<IDocumentService, BlobDocumentService>();
builder.Services.AddScoped<IPaymentGateway, PaymentGatewayStub>();
builder.Services.AddScoped<IAuditService, AuditService>();
builder.Services.AddScoped<IUserContextService, UserContextService>();

builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(options =>
    {
        options.Authority = builder.Configuration["Jwt:Authority"];
        options.Audience = builder.Configuration["Jwt:Audience"];
        options.RequireHttpsMetadata = true;
        options.TokenValidationParameters = new TokenValidationParameters
        {
            NameClaimType = "name",
            RoleClaimType = "roles",
            ValidateIssuer = true,
            ValidateAudience = true,
            ValidateLifetime = true
        };
    });

builder.Services.AddAuthorization();

var app = builder.Build();

using (var scope = app.Services.CreateScope())
{
    var db = scope.ServiceProvider.GetRequiredService<AppDbContext>();
    var logger = scope.ServiceProvider.GetRequiredService<ILoggerFactory>().CreateLogger("Startup");
    var failOnMigrationError = app.Configuration.GetValue<bool>("Startup:FailOnMigrationError");

    try
    {
        await db.Database.MigrateAsync();

        var seedEnabled = app.Configuration.GetValue<bool>("SeedData:Enabled") || app.Environment.IsDevelopment();
        if (seedEnabled)
        {
            await SeedData.ApplyAsync(db, logger, CancellationToken.None);
        }

        startupState.IsDatabaseReady = true;
        startupState.LastStartupError = null;
    }
    catch (Exception ex)
    {
        startupState.IsDatabaseReady = false;
        startupState.LastStartupError = "Database migration failed during startup.";
        logger.LogError(ex, "Database migration failed during startup.");

        if (failOnMigrationError)
        {
            throw;
        }
    }
}

if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.UseHttpsRedirection();
app.UseAuthentication();
app.UseAuthorization();

app.MapControllers();

app.Run();
