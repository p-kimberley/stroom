/*
 * Stroom Auth API
 * Various APIs for interacting with authentication, users, and tokens.
 *
 * OpenAPI spec version: v1
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */


package stroom.authentication.service.api.model;

import java.util.Objects;
import com.google.gson.annotations.SerializedName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * A request to change a user&#39;s password.
 */
@ApiModel(description = "A request to change a user's password.")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2019-11-26T10:17:08.903Z")
public class ChangePasswordRequest {
  @SerializedName("newPassword")
  private String newPassword = null;

  @SerializedName("oldPassword")
  private String oldPassword = null;

  @SerializedName("email")
  private String email = null;

  public ChangePasswordRequest newPassword(String newPassword) {
    this.newPassword = newPassword;
    return this;
  }

   /**
   * The new password.
   * @return newPassword
  **/
  @ApiModelProperty(example = "null", required = true, value = "The new password.")
  public String getNewPassword() {
    return newPassword;
  }

  public void setNewPassword(String newPassword) {
    this.newPassword = newPassword;
  }

  public ChangePasswordRequest oldPassword(String oldPassword) {
    this.oldPassword = oldPassword;
    return this;
  }

   /**
   * The old password.
   * @return oldPassword
  **/
  @ApiModelProperty(example = "null", required = true, value = "The old password.")
  public String getOldPassword() {
    return oldPassword;
  }

  public void setOldPassword(String oldPassword) {
    this.oldPassword = oldPassword;
  }

  public ChangePasswordRequest email(String email) {
    this.email = email;
    return this;
  }

   /**
   * The email address of the user we're changing the password for.
   * @return email
  **/
  @ApiModelProperty(example = "null", required = true, value = "The email address of the user we're changing the password for.")
  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ChangePasswordRequest changePasswordRequest = (ChangePasswordRequest) o;
    return Objects.equals(this.newPassword, changePasswordRequest.newPassword) &&
        Objects.equals(this.oldPassword, changePasswordRequest.oldPassword) &&
        Objects.equals(this.email, changePasswordRequest.email);
  }

  @Override
  public int hashCode() {
    return Objects.hash(newPassword, oldPassword, email);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ChangePasswordRequest {\n");
    
    sb.append("    newPassword: ").append(toIndentedString(newPassword)).append("\n");
    sb.append("    oldPassword: ").append(toIndentedString(oldPassword)).append("\n");
    sb.append("    email: ").append(toIndentedString(email)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
  
}
